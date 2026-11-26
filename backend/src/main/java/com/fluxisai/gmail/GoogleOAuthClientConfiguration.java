package com.fluxisai.gmail;

import java.time.Clock;
import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class GoogleOAuthClientConfiguration {

    @Bean
    GoogleOAuthClient googleOAuthClient(GmailOAuthProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(10));

        RestClient restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();

        return new RestClientGoogleOAuthClient(restClient, properties, Clock.systemUTC());
    }
}