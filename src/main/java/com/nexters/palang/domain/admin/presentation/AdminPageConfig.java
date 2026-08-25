package com.nexters.palang.domain.admin.presentation;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// 정적 리소스 핸들러는 "/admin"·"/admin/"(트레일링 슬래시) 요청을 디렉터리 내 index.html로 자동
// 연결해주지 않아 NoResourceFoundException(500)이 발생한다. 관리자가 외우기 쉬운 짧은 경로로도
// 페이지에 들어올 수 있도록 정적 파일의 실제 경로로 리다이렉트한다.
@Configuration
public class AdminPageConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addRedirectViewController("/admin", "/admin/index.html");
        registry.addRedirectViewController("/admin/", "/admin/index.html");
    }
}
