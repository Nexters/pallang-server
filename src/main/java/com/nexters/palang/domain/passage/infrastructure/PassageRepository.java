package com.nexters.palang.domain.passage.infrastructure;

import com.nexters.palang.domain.passage.domain.Passage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PassageRepository extends JpaRepository<Passage, Long> {

    boolean existsByIdAndDeletedAtIsNull(Long id);
}
