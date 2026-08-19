package com.nexters.palang.domain.group.common.error;

import com.nexters.palang.global.common.error.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum GroupErrorCode implements BaseErrorCode {

    GROUP_NOT_FOUND(HttpStatus.NOT_FOUND, "GROUP_404_1", "해당 모임을 찾을 수 없습니다."),
    INVALID_INVITE_CODE(HttpStatus.NOT_FOUND, "GROUP_404_2", "유효하지 않은 초대 코드입니다."),
    INVALID_GROUP_PERIOD(HttpStatus.BAD_REQUEST, "GROUP_400_1", "시작일은 종료일보다 늦을 수 없습니다."),
    INVALID_GROUP_CAPACITY(HttpStatus.BAD_REQUEST, "GROUP_400_2", "인원은 2명 이상 10명 이하여야 합니다."),
    NOT_HOST(HttpStatus.FORBIDDEN, "GROUP_403_1", "모임장만 가능한 작업입니다."),
    NOT_MEMBER(HttpStatus.FORBIDDEN, "GROUP_403_2", "모임원만 조회할 수 있습니다."),
    CAPACITY_BELOW_MEMBER_COUNT(HttpStatus.CONFLICT, "GROUP_409_1", "인원을 현재 참여 인원보다 적게 설정할 수 없습니다."),
    ALREADY_JOINED(HttpStatus.CONFLICT, "GROUP_409_2", "이미 가입된 모임입니다."),
    GROUP_FULL(HttpStatus.CONFLICT, "GROUP_409_3", "모임 정원이 가득 찼습니다."),
    ;

    private final HttpStatus httpStatus;
    private final String customCode;
    private final String message;
}
