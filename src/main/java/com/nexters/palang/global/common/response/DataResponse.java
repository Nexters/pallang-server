package com.nexters.palang.global.common.response;

import lombok.Getter;

@Getter
public class DataResponse<T> extends BaseResponse {

    private final T data;

    private DataResponse(T data) {
        this.data = data;
    }

    public static <T> DataResponse<T> from(T data) {
        return new DataResponse<>(data);
    }
}
