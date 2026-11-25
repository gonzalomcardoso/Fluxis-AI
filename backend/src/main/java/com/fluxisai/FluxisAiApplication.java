package com.fluxisai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication
public class FluxisAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(FluxisAiApplication.class, args);
    }
}
