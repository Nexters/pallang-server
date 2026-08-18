package com.nexters.palang.domain.group.infrastructure;

import com.nexters.palang.domain.group.domain.Group;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupRepository extends JpaRepository<Group, Long> {
}
