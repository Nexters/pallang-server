package com.nexters.palang.domain.notice.infrastructure;

import com.nexters.palang.domain.notice.domain.Notice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    Page<Notice> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
