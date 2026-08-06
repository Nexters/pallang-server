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

    // MySQL/H2 모두 제약 위반 예외 메시지에 제약 이름(User.uq_users_nickname)을 포함하므로, 이 문자열로
    // "진짜 닉네임 충돌"과 그 외 무결성 위반(예: 컬럼 nullable 설정이 스키마와 어긋난 NOT NULL 위반)을 구분한다.
    // (실제로 terms_agreed_at 컬럼이 엔티티는 nullable인데 DB에는 NOT NULL로 남아있던 사고에서, 이 구분이
    // 없어 원인과 무관한 "닉네임 생성 실패"로 잘못 보고된 적이 있다.)
    private static final String NICKNAME_UNIQUE_CONSTRAINT = "uq_users_nickname";

    // FR-AUTH-04: 닉네임 유니크 제약 충돌은 사전 조회 대신 저장 시도 -> 실패 시 접미사를 붙여 재시도하는
    // 낙관적 방식으로 처리한다(사전 조회는 동시성에 취약). 각 시도를 REQUIRES_NEW로 독립된 트랜잭션에서
    // 실행해, 제약 위반 예외가 영속성 컨텍스트를 오염시켜 다음 시도까지 실패시키는 것을 막는다.
    public User registerViaSns(SnsProvider snsProvider, String snsId, String email, String name) {
        String base = nicknameGenerator.generateBase();
        for (int suffix = 0; suffix <= NicknameGenerator.MAX_SUFFIX_ATTEMPTS; suffix++) {
            String nickname = suffix == 0 ? base : nicknameGenerator.withSuffix(base, suffix);
            try {
                return createUser(nickname, snsProvider, snsId, email, name);
            } catch (DataIntegrityViolationException e) {
                if (!isNicknameConflict(e)) {
                    // 닉네임 충돌이 아닌 다른 무결성 위반을 닉네임 생성 실패로 오인해 삼키지 않는다 —
                    // 그대로 던져서 GlobalExceptionHandler가 500과 실제 원인을 로그로 남기게 한다.
                    throw e;
                }
                // 닉네임 충돌로 보고 다음 접미사로 재시도한다.
            }
        }
        throw new UserException(UserErrorCode.NICKNAME_GENERATION_FAILED);
    }

    // 탈퇴한 계정이 같은 SNS 식별자로 재로그인한 경우: sns_provider+sns_id 유니크 제약 때문에 새 로우를
    // 만들 수 없어 기존 로우를 초기화해 재가입시킨다. 닉네임 충돌 재시도 로직은 registerViaSns와 동일하다.
    public User reactivate(User user, String email, String name) {
        String base = nicknameGenerator.generateBase();
        for (int suffix = 0; suffix <= NicknameGenerator.MAX_SUFFIX_ATTEMPTS; suffix++) {
            String nickname = suffix == 0 ? base : nicknameGenerator.withSuffix(base, suffix);
            try {
                return saveReactivatedUser(user, nickname, email, name);
            } catch (DataIntegrityViolationException e) {
                if (!isNicknameConflict(e)) {
                    throw e;
                }
            }
        }
        throw new UserException(UserErrorCode.NICKNAME_GENERATION_FAILED);
    }

    private boolean isNicknameConflict(DataIntegrityViolationException e) {
        String message = String.valueOf(e.getMostSpecificCause().getMessage()).toLowerCase();
        return message.contains(NICKNAME_UNIQUE_CONSTRAINT);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public User createUser(String nickname, SnsProvider snsProvider, String snsId, String email, String name) {
        User user = User.builder()
                .nickname(nickname)
                .snsProvider(snsProvider)
                .snsId(snsId)
                .email(email)
                .name(name)
                .build();
        return userRepository.saveAndFlush(user);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public User saveReactivatedUser(User user, String nickname, String email, String name) {
        user.reactivate(nickname, email, name);
        return userRepository.saveAndFlush(user);
    }
}
