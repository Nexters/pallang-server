package com.nexters.palang.global.idempotency;

// 요청 키를 구분하는 작업 종류. 같은 키라도 작업이 다르면 별개로 취급한다.
// 이후 단계에서 합치기(#227)가 추가된다.
public enum IdempotentOperation {
    CREATE_GROUP,
    ADD_GROUP_BOOK,
    REMOVE_GROUP_MEMBER,
}
