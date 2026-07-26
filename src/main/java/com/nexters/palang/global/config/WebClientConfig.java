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

    @Bean
    public WebClient aladinWebClient(WebClient.Builder builder, @Value("${aladin.base-url}") String baseUrl) {
        return webClient(builder, baseUrl);
    }

    @Bean
    public WebClient kakaoWebClient(WebClient.Builder builder, @Value("${kakao.base-url}") String baseUrl) {
        return webClient(builder, baseUrl);
    }

    private WebClient webClient(WebClient.Builder builder, String baseUrl) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT_MILLIS)
                .responseTimeout(RESPONSE_TIMEOUT)
                .doOnConnected(conn -> conn.addHandlerLast(
                        new ReadTimeoutHandler(RESPONSE_TIMEOUT.getSeconds(), TimeUnit.SECONDS)));

        return builder.baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
