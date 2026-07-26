package com.nexters.palang.domain.user.application;

import com.nexters.palang.domain.user.common.error.UserErrorCode;
import com.nexters.palang.domain.user.common.error.UserException;
import com.nexters.palang.domain.user.domain.SnsProvider;
import com.nexters.palang.domain.user.domain.User;
import com.nexters.palang.domain.user.infrastructure.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class UserRegistrationService {

    private final UserRepository userRepository;
    private final NicknameGenerator nicknameGenerator;

    // FR-AUTH-04: 닉네임 유니크 제약 충돌은 사전 조회 대신 저장 시도 -> 실패 시 접미사를 붙여 재시도하는
    // 낙관적 방식으로 처리한다(사전 조회는 동시성에 취약). 각 시도를 REQUIRES_NEW로 독립된 트랜잭션에서
    // 실행해, 제약 위반 예외가 영속성 컨텍스트를 오염시켜 다음 시도까지 실패시키는 것을 막는다.
    public User registerViaSns(SnsProvider snsProvider, String snsId) {
        String base = nicknameGenerator.generateBase();
        for (int suffix = 0; suffix <= NicknameGenerator.MAX_SUFFIX_ATTEMPTS; suffix++) {
            String nickname = suffix == 0 ? base : nicknameGenerator.withSuffix(base, suffix);
            try {
                return createUser(nickname, snsProvider, snsId);
            } catch (DataIntegrityViolationException e) {
                // 닉네임 충돌로 보고 다음 접미사로 재시도한다.
            }
        }
        throw new UserException(UserErrorCode.NICKNAME_GENERATION_FAILED);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public User createUser(String nickname, SnsProvider snsProvider, String snsId) {
        User user = User.builder()
                .nickname(nickname)
                .snsProvider(snsProvider)
                .snsId(snsId)
                .build();
        return userRepository.saveAndFlush(user);
    }
}
