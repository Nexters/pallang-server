package com.nexters.palang.domain.decoration.infrastructure;

import com.nexters.palang.domain.decoration.domain.Decoration;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DecorationRepository extends JpaRepository<Decoration, Long> {

    // 관리자 유저 삭제(AdminUserService): 삭제 대상 의견에 달린 데코 전부.
    void deleteAllByOpinionIdIn(List<Long> opinionIds);
}
