package com.nexters.palang.global.storage;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    // 파일을 저장하고, 외부에서 접근 가능한 공개 URL을 반환합니다.
    String store(MultipartFile file, String subDirectory);
}
