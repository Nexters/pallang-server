package com.nexters.palang.global.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Configuration
public class WebClientConfig {

    private static final int CONNECT_TIMEOUT_MILLIS = 3_000;
    private static final Duration RESPONSE_TIMEOUT = Duration.ofSeconds(10);
    // 도서 검색은 타이핑 중 자동완성으로도 호출되므로, 알라딘이 느릴 때 오래 기다리지 않도록
    // 응답 타임아웃을 짧게 둔다 (다른 외부 연동은 기존 10초 유지).
    private static final Duration ALADIN_RESPONSE_TIMEOUT = Duration.ofSeconds(3);

    @Bean
    public WebClient aladinWebClient(WebClient.Builder builder, @Value("${aladin.base-url}") String baseUrl) {
        return webClient(builder, baseUrl, ALADIN_RESPONSE_TIMEOUT);
    }

    @Bean
    public WebClient kakaoWebClient(WebClient.Builder builder, @Value("${kakao.base-url}") String baseUrl) {
        return webClient(builder, baseUrl, RESPONSE_TIMEOUT);
    }

    @Bean
    public WebClient appleWebClient(WebClient.Builder builder, @Value("${apple.base-url}") String baseUrl) {
        return webClient(builder, baseUrl, RESPONSE_TIMEOUT);
    }

    private WebClient webClient(WebClient.Builder builder, String baseUrl, Duration responseTimeout) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT_MILLIS)
                .responseTimeout(responseTimeout)
                .doOnConnected(conn -> conn.addHandlerLast(
                        new ReadTimeoutHandler(responseTimeout.getSeconds(), TimeUnit.SECONDS)));

        return builder.baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
