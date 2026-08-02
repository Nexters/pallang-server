package com.nexters.palang.global.storage;

import com.nexters.palang.global.common.error.AppException;
import com.nexters.palang.global.common.error.GlobalErrorCode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class LocalFileStorageService implements FileStorageService {

    private final Path uploadDir;
    private final String baseUrl;

    public LocalFileStorageService(
            @Value("${storage.upload-dir}") String uploadDir,
            @Value("${storage.base-url}") String baseUrl) {
        this.uploadDir = Path.of(uploadDir).toAbsolutePath().normalize();
        this.baseUrl = baseUrl;
    }

    @Override
    public String store(MultipartFile file, String subDirectory) {
        ImageMimeType mimeType = ImageMimeType.from(file.getContentType())
                .orElseThrow(() -> new AppException(GlobalErrorCode.INVALID_INPUT_VALUE));
        String filename = UUID.randomUUID() + "." + mimeType.extension();

        try {
            Path targetDir = uploadDir.resolve(subDirectory).normalize();
            Files.createDirectories(targetDir);
            file.transferTo(targetDir.resolve(filename));
        } catch (IOException e) {
            throw new AppException(GlobalErrorCode.INTERNAL_SERVER_ERROR);
        }

        return baseUrl + "/" + subDirectory + "/" + filename;
    }
}
