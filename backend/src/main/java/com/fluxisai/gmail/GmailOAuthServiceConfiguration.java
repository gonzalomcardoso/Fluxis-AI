package com.fluxisai.gmail;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GmailOAuthServiceConfiguration {

    @Bean(name = "gmailOAuthClock")
    Clock gmailOAuthClock() {
        return Clock.systemUTC();
    }
}