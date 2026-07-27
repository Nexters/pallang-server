package com.nexters.palang.global.config;

import com.nexters.palang.domain.book.domain.Book;
import com.nexters.palang.domain.book.domain.BookSource;
import com.nexters.palang.domain.book.infrastructure.BookRepository;
import com.nexters.palang.domain.passage.domain.Passage;
import com.nexters.palang.domain.passage.infrastructure.PassageRepository;
import com.nexters.palang.domain.policy.domain.Policy;
import com.nexters.palang.domain.policy.domain.PolicyType;
import com.nexters.palang.domain.policy.infrastructure.PolicyRepository;
import com.nexters.palang.domain.user.domain.SnsProvider;
import com.nexters.palang.domain.user.domain.User;
import com.nexters.palang.domain.user.infrastructure.UserRepository;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// 로컬 개발/FE 협업용 초기 데이터. @DataJpaTest/@WebMvcTest 슬라이스 테스트는 이 빈을 로드하지 않고,
// @SpringBootTest 통합 테스트는 자체 데이터를 직접 만들어 검증하므로 이 시드와 충돌하지 않는다.
// (data.sql 방식은 컨텍스트가 캐시·공유되는 @DataJpaTest 전반을 오염시켜 채택하지 않음)
@Profile("local")
@Component
@RequiredArgsConstructor
public class LocalDataSeeder implements ApplicationRunner {

    private static final String SEED_SNS_ID = "seed-user";

    private final BookRepository bookRepository;
    private final PassageRepository passageRepository;
    private final UserRepository userRepository;
    private final PolicyRepository policyRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedPolicies();
        seedBooks();
    }

    private void seedPolicies() {
        seedPolicyIfAbsent(PolicyType.TERMS, TERMS_MARKDOWN);
        seedPolicyIfAbsent(PolicyType.PRIVACY, PRIVACY_MARKDOWN);
    }

    private void seedPolicyIfAbsent(PolicyType type, String content) {
        if (policyRepository.findByType(type).isEmpty()) {
            policyRepository.save(Policy.builder().type(type).content(content).build());
        }
    }

    private void seedBooks() {
        if (bookRepository.count() > 0) {
            return;
        }

        User seedUser = userRepository.save(User.builder()
                .nickname("따스한책갈피")
                .snsProvider(SnsProvider.KAKAO)
                .snsId(SEED_SNS_ID)
                .build());

        List<Book> books = bookRepository.saveAll(List.of(
                book("달러구트 꿈 백화점", "이미예", "팩토리나인", 300,
                        "https://picsum.photos/seed/book1/300/450"),
                book("불편한 편의점", "김호연", "나무옆의자", 320,
                        "https://picsum.photos/seed/book2/300/450"),
                book("아몬드", "손원평", "창비", 264,
                        "https://picsum.photos/seed/book3/300/450"),
                book("죽고 싶지만 떡볶이는 먹고 싶어", "백세희", "흔", 260,
                        "https://picsum.photos/seed/book4/300/450"),
                book("여행의 이유", "김영하", "문학동네", 256,
                        "https://picsum.photos/seed/book5/300/450"),
                book("미드나잇 라이브러리", "매트 헤이그", "인플루엔셜", 428,
                        "https://picsum.photos/seed/book6/300/450")
        ));

        for (Book book : books) {
            passageRepository.save(passage(book, seedUser));
        }
    }

    private Book book(String title, String author, String publisher, int pageCount, String coverImageUrl) {
        return Book.builder()
                .title(title)
                .author(author)
                .publisher(publisher)
                .pageCount(pageCount)
                .coverImageUrl(coverImageUrl)
                .source(BookSource.MANUAL)
                .build();
    }

    private Passage passage(Book book, User creator) {
        String quotedText = "\"" + book.getTitle() + "\"에서 발췌한 첫 문장입니다.";
        return Passage.builder()
                .book(book)
                .creator(creator)
                .pageNumber(1)
                .quotedText(quotedText)
                .isSpoiler(false)
                .normalizedHash(normalize(quotedText))
                .build();
    }

    private String normalize(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.replaceAll("[\\s\\p{Punct}]", "").getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : hash) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static final String TERMS_MARKDOWN = """
            # 이용약관

            ## 1. 서비스 목적
            본 서비스는 사용자가 책의 문장을 발췌하고 자신의 의견을 기록하며, 다른 이용자의 생각을 익명으로 나누는 디지털 교환독서 서비스입니다.

            ## 2. 계정과 닉네임
            사용자는 서비스에서 실명 대신 익명 닉네임으로 활동합니다. 가입 시 서비스는 임시 닉네임을 자동으로 부여하며, 사용자는 설정 화면에서 원하는 닉네임을 직접 입력하여 변경할 수 있습니다.

            닉네임은 다른 이용자가 이미 사용 중인 이름과 중복될 수 없으며, 욕설·혐오 표현·타인의 권리를 침해하거나 서비스 운영 정책에 위반되는 표현은 사용할 수 없습니다. 닉네임 변경은 하루 1회로 제한되며, 변경 가능 시점은 설정 화면에서 안내합니다.

            변경된 닉네임은 앱 전체의 발췌, 의견, 댓글 등 사용자 활동에 동일하게 표시됩니다. 서비스는 운영 정책 위반 또는 신고 검토 결과에 따라 특정 닉네임의 변경을 요청하거나 이용을 제한할 수 있습니다.

            ## 3. 사용자 콘텐츠와 저작권
            사용자는 직접 작성한 의견과 댓글에 대한 책임을 집니다. 책의 원문, 페이지 이미지, 발췌는 저작권 보호를 위해 서비스가 정한 글자 수와 노출 범위를 따라야 하며, 전체 페이지 이미지나 과도한 원문 공유는 제한됩니다.

            ## 4. 금지 행위
            타인에 대한 비방·혐오·성적 표현·개인정보 공개·불법 정보 게시, 저작권 침해, 서비스 운영을 방해하는 행위는 금지됩니다. 서비스는 신고된 콘텐츠를 검토하여 숨김, 삭제, 이용 제한 등의 조치를 할 수 있습니다.

            ## 5. 콘텐츠의 표시
            사용자가 공개 범위 내에서 작성한 발췌·의견·댓글은 다른 이용자에게 표시될 수 있습니다. 서비스는 운영, 오류 대응, 분쟁 처리, 서비스 개선을 위해 필요한 범위에서 콘텐츠를 저장하고 관리합니다.

            ## 6. 서비스 변경 및 책임 제한
            서비스는 운영상 필요한 경우 기능이나 정책을 변경할 수 있으며, 중요한 변경은 앱 내 공지 또는 알림으로 안내합니다. 천재지변, 통신 장애, 이용자 귀책 사유 등 서비스가 통제하기 어려운 사유로 발생한 손해에 대해서는 책임이 제한될 수 있습니다.

            ## 7. 문의 및 분쟁 처리
            서비스 이용 관련 문의는 [문의 이메일]로 접수합니다. 서비스와 이용자 간 분쟁은 관련 법령과 절차에 따라 해결합니다.
            """;

    private static final String PRIVACY_MARKDOWN = """
            # 개인정보 처리방침

            ## 1. 수집하는 개인정보
            서비스는 회원 가입과 이용을 위해 이메일 주소 또는 소셜 로그인 식별자, 자동 배정된 익명 닉네임, 앱 이용 기록, 기기 정보, 알림 토큰, 문의 내용 등을 수집할 수 있습니다. 사용자가 발췌·의견 작성 과정에서 입력한 문장, 페이지 번호, 감정 태그, 댓글 등의 콘텐츠도 서비스 제공을 위해 저장됩니다.

            ## 2. 개인정보의 이용 목적
            수집한 정보는 회원 식별 및 계정 관리, 발췌·의견·독서 진행도 저장, 알림 발송, 부정 이용 방지, 문의 대응, 서비스 개선을 위해 이용합니다. 서비스는 이용자의 동의 없이 개인정보를 판매하거나 광고 목적으로 제공하지 않습니다.

            ## 3. 보유 및 파기
            개인정보는 회원 탈퇴 또는 수집 목적 달성 시 지체 없이 파기합니다. 다만 관계 법령상 보관이 필요한 정보는 해당 기간 동안 보관할 수 있습니다. 탈퇴 시 공개된 발췌와 의견은 다른 이용자의 대화 맥락 유지를 위해 익명화하여 남길 수 있으며, 이 기준은 탈퇴 화면에서 명확히 안내합니다.

            ## 4. 이용자의 권리
            이용자는 자신의 개인정보를 열람·정정·삭제하거나 처리 정지를 요청할 수 있습니다. 요청은 앱 내 문의 또는 서비스 문의처를 통해 할 수 있으며, 회사는 관련 법령에 따라 처리합니다.

            ## 5. 개인정보 처리 위탁 및 제3자 제공
            서비스 운영에 필요한 클라우드 저장소, 알림 발송, 오류 분석 도구를 사용하는 경우 해당 업체와 필요한 범위에서 개인정보 처리 업무를 위탁할 수 있습니다. 제3자 제공 또는 해외 이전이 발생하면 대상, 목적, 보유 기간을 별도로 안내하고 필요한 동의를 받습니다.

            ## 6. 문의처
            개인정보 관련 문의는 [문의 이메일]로 접수합니다. 개인정보 보호책임자는 [담당자 이름 또는 부서], 연락처는 [연락처]로 표시합니다.
            """;
}
