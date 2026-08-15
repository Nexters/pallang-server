package com.nexters.palang.global.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nexters.palang.global.common.error.AppException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class LocalFileStorageServiceTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("이미지를 저장하면 하위 디렉터리에 파일이 생기고 public-base-url 기준 절대 URL을 반환한다")
    void storeSavesFileAndReturnsUrl() throws IOException {
        LocalFileStorageService storageService =
                new LocalFileStorageService(tempDir.toString(), "https://api.example.com/images");
        MockMultipartFile file = new MockMultipartFile("coverImage", "cover.jpg", "image/jpeg", "data".getBytes());

        String url = storageService.store(file, "book-covers");

        assertThat(url).startsWith("https://api.example.com/images/book-covers/").endsWith(".jpg");
        String filename = url.substring(url.lastIndexOf('/') + 1);
        assertThat(Files.exists(tempDir.resolve("book-covers").resolve(filename))).isTrue();
    }

    @Test
    @DisplayName("이미지 타입이 아니면 예외가 발생하고 파일을 저장하지 않는다")
    void storeFailsWhenContentTypeIsNotImage() {
        LocalFileStorageService storageService =
                new LocalFileStorageService(tempDir.toString(), "https://api.example.com/images");
        MockMultipartFile file = new MockMultipartFile("coverImage", "cover.txt", "text/plain", "data".getBytes());

        assertThatThrownBy(() -> storageService.store(file, "book-covers")).isInstanceOf(AppException.class);
    }
}
