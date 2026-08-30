package com.nexters.palang.domain.book.infrastructure;

import com.nexters.palang.domain.book.domain.Book;
import com.nexters.palang.domain.book.domain.BookSource;
import com.nexters.palang.domain.book.domain.SampleLibraryBook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// BookService#sampleLibraryPage(홈 고정 샘플 도서, 이슈 #119)는 ISBN으로 book row를 찾아 그 id를
// 쓴다. 이 책이 dev에서는 실사용 중 우연히 만들어졌지만 prod에는 만들어진 적이 없어 row 자체가
// 없었고, 그 결과 샘플 카드를 눌러 상세로 들어가면 404가 나는 문제가 있었다(#166). 앱 시작 시 이
// 책이 없으면 한 번 만들어 둬서, 어느 환경에서든 항상 유효한 book id를 참조하도록 한다. 이미 있으면
// 아무 일도 하지 않으므로 재배포/재기동해도 안전하다(멱등적).
@Slf4j
@Component
@RequiredArgsConstructor
public class SampleLibraryBookSeeder implements ApplicationRunner {

    private final BookRepository bookRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (bookRepository.findByIsbn(SampleLibraryBook.ISBN).isPresent()) {
            return;
        }
        Book book = Book.builder()
                .title(SampleLibraryBook.TITLE)
                .author(SampleLibraryBook.AUTHOR)
                .publisher(SampleLibraryBook.PUBLISHER)
                .pageCount(SampleLibraryBook.PAGE_COUNT)
                .isbn(SampleLibraryBook.ISBN)
                .coverImageUrl(SampleLibraryBook.COVER_IMAGE_URL)
                .source(BookSource.API)
                .build();
        bookRepository.save(book);
        log.info("홈 고정 샘플 도서 row 생성 완료 (isbn={})", SampleLibraryBook.ISBN);
    }
}
