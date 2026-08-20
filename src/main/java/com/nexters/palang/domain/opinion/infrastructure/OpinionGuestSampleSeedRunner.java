package com.nexters.palang.domain.opinion.infrastructure;

import com.nexters.palang.domain.book.domain.Book;
import com.nexters.palang.domain.book.infrastructure.BookRepository;
import com.nexters.palang.domain.decoration.domain.Decoration;
import com.nexters.palang.domain.decoration.domain.EffectType;
import com.nexters.palang.domain.opinion.domain.Opinion;
import com.nexters.palang.domain.passage.application.PassageNormalizer;
import com.nexters.palang.domain.passage.domain.Passage;
import com.nexters.palang.domain.passage.infrastructure.PassageRepository;
import com.nexters.palang.domain.user.domain.GuestSampleAccount;
import com.nexters.palang.domain.user.domain.User;
import com.nexters.palang.domain.user.infrastructure.UserRepository;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

// 비로그인 "내 의견" 미리보기(이슈 #120)를 하드코딩 대신 실제 계정의 실제 흔적/꾸밈으로 보여주기 위해,
// 앱 시작 시 한 번 샘플 계정과 그 계정의 흔적 2건(대목 2개)을 만들어둔다. 이 프로젝트엔 Flyway 등
// 마이그레이션 도구가 없어 BookTitleNormalizationBackfillRunner와 같은 방식(기동 시 자동 실행)을 따른다.
// 샘플 계정에 이미 흔적이 있으면(=이미 씨딩됨) 아무 일도 하지 않으므로 재배포/재기동해도 안전하다(멱등적).
// bookId=18("빵충 사육 준수 사항")이 없는 환경(로컬/테스트 DB 등)에서는 조용히 건너뛴다 — 이 도서는
// BookService.GUEST_SAMPLE_LIBRARY_BOOK에서도 이미 같은 방식으로 하드코딩되어 있는 전제다.
@Slf4j
@Component
public class OpinionGuestSampleSeedRunner implements ApplicationRunner {

    private static final Long SAMPLE_BOOK_ID = 18L;

    private static final int SAMPLE_PAGE_33 = 33;
    private static final String SAMPLE_QUOTE_33 = "이 모든 건 챗지피티 덕분이었다. 담당자가 준 자료를 복사해서 "
            + "이 녀석에게 붙여넣기를 반복하니 한눈에 봐도 괜찮은 서류를 달칵 토해냈다. 세상에, AI가 사람을 구했어요.";
    private static final String SAMPLE_OPINION_33 = "애증의 관계";
    // "세상에, AI가 사람을 구했어요." 구간에 동그라미(빨간색) 표시.
    private static final int CIRCLE_START = 77;
    private static final int CIRCLE_END = 95;

    private static final int SAMPLE_PAGE_70 = 70;
    private static final String SAMPLE_QUOTE_70 = "빵충은 번데기로 변태하는 순간 영양 성분이 급격히 변질되며, 특유의 향과 식감이 사라져 "
            + "상품 가치가 사라집니다. 또한 번데기에서는 다량의 온실가스가 배출되어 기후 변화에 악영향을 미칠 가능성이 발견되었습니다.";
    private static final List<String> SAMPLE_OPINIONS_70 = List.of("응 ???");

    private static final String DECORATION_COLOR = "#FF0000";

    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final PassageRepository passageRepository;
    private final OpinionRepository opinionRepository;
    private final TransactionTemplate transactionTemplate;

    @Autowired
    public OpinionGuestSampleSeedRunner(
            UserRepository userRepository,
            BookRepository bookRepository,
            PassageRepository passageRepository,
            OpinionRepository opinionRepository,
            PlatformTransactionManager transactionManager) {
        this.userRepository = userRepository;
        this.bookRepository = bookRepository;
        this.passageRepository = passageRepository;
        this.opinionRepository = opinionRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public void run(ApplicationArguments args) {
        boolean seeded = Boolean.TRUE.equals(transactionTemplate.execute(status -> {
            User sampleUser = userRepository
                    .findBySnsProviderAndSnsId(GuestSampleAccount.SNS_PROVIDER, GuestSampleAccount.SNS_ID)
                    .orElseGet(this::createSampleUser);

            if (opinionRepository.countByUserIdAndDeletedAtIsNull(sampleUser.getId()) > 0) {
                return false;
            }

            Book book = bookRepository.findById(SAMPLE_BOOK_ID).orElse(null);
            if (book == null) {
                log.info("샘플 도서(id={})가 없어 비로그인 미리보기 흔적 씨딩을 건너뜁니다.", SAMPLE_BOOK_ID);
                return false;
            }

            Passage passage33 = createPassage(book, sampleUser, SAMPLE_PAGE_33, SAMPLE_QUOTE_33);
            Opinion opinion33 = Opinion.createWithDecorations(passage33, sampleUser, SAMPLE_OPINION_33,
                    List.of(Decoration.builder()
                            .startOffset(CIRCLE_START).endOffset(CIRCLE_END)
                            .effectType(EffectType.CIRCLE).color(DECORATION_COLOR)
                            .build()));
            opinionRepository.save(opinion33);

            Passage passage70 = createPassage(book, sampleUser, SAMPLE_PAGE_70, SAMPLE_QUOTE_70);
            for (String content : SAMPLE_OPINIONS_70) {
                Opinion opinion70 = Opinion.createWithDecorations(passage70, sampleUser, content,
                        List.of(Decoration.builder()
                                .startOffset(0).endOffset(SAMPLE_QUOTE_70.length())
                                .effectType(EffectType.UNDERLINE).color(DECORATION_COLOR)
                                .build()));
                opinionRepository.save(opinion70);
            }

            return true;
        }));

        if (seeded) {
            log.info("비로그인 미리보기용 샘플 계정/흔적 씨딩 완료");
        }
    }

    private User createSampleUser() {
        User user = User.builder()
                .nickname(GuestSampleAccount.NICKNAME)
                .snsProvider(GuestSampleAccount.SNS_PROVIDER)
                .snsId(GuestSampleAccount.SNS_ID)
                .build();
        return userRepository.save(user);
    }

    private Passage createPassage(Book book, User creator, int pageNumber, String quotedText) {
        Passage passage = Passage.builder()
                .book(book)
                .creator(creator)
                .group(null)
                .pageNumber(pageNumber)
                .quotedText(quotedText)
                .isSpoiler(false)
                .normalizedHash(PassageNormalizer.normalizedHash(quotedText))
                .build();
        return passageRepository.save(passage);
    }
}
