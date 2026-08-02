package com.nexters.palang.global.storage;

import java.util.Arrays;
import java.util.Optional;
import org.springframework.http.MediaType;

// 이미지 업로드를 지원하는 도메인(Book, User 등)에서 공통으로 쓰는 콘텐츠 타입 검증/확장자 매핑입니다.
public enum ImageMimeType {

    JPEG(MediaType.IMAGE_JPEG_VALUE, "jpg"),
    PNG(MediaType.IMAGE_PNG_VALUE, "png"),
    ;

    private final String contentType;
    private final String extension;

    ImageMimeType(String contentType, String extension) {
        this.contentType = contentType;
        this.extension = extension;
    }

    public String extension() {
        return extension;
    }

    public static Optional<ImageMimeType> from(String contentType) {
        return Arrays.stream(values())
                .filter(type -> type.contentType.equals(contentType))
                .findFirst();
    }
}
