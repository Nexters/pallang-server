package com.nexters.palang.global.config;

import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// 업로드된 이미지(book-covers, profile-images 등)를 정적 리소스로 서빙한다.
// nginx는 전체 트래픽을 앱으로 프록시하므로 별도 nginx 설정 없이 이 핸들러가 처리한다.
@Configuration
public class WebStorageConfig implements WebMvcConfigurer {

    private final String uploadDir;
    private final String baseUrl;

    public WebStorageConfig(
            @Value("${storage.upload-dir}") String uploadDir,
            @Value("${storage.base-url}") String baseUrl) {
        this.uploadDir = uploadDir;
        this.baseUrl = baseUrl;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Path.of(uploadDir).toAbsolutePath().normalize() + "/";
        registry.addResourceHandler(baseUrl + "/**")
                .addResourceLocations("file:" + location);
    }
}
