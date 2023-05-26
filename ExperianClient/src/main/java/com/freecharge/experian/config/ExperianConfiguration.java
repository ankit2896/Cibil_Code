package com.freecharge.experian.config;

import feign.codec.Encoder;
import feign.form.FormEncoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class ExperianConfiguration {

    @Bean
    public Encoder encoder() {
        return new FormEncoder();

    }

}