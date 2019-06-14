package com.dnastack.wes.v1.client;


import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Client;
import feign.Feign;
import feign.Logger;
import feign.Logger.Level;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.form.FormEncoder;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import feign.okhttp.OkHttpClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Customized Bean configuration for the Feign clients.
 */
@Slf4j
@Configuration
public class ClientConfigurations {

    @Autowired
    private ObjectMapper mapper;

    @Bean
    public Encoder encoder() {
        return new FormEncoder(new JacksonEncoder(mapper));
    }

    @Bean
    public Decoder decoder() {
        return new JacksonDecoder(mapper);
    }


    @Bean
    public CromwellClient cromwellClient(CromwellAuthRequestInterceptor authRequestInterceptor, CromwellConfig cromwellConfig) {
        Client httpClient = new OkHttpClient();

        return Feign.builder().client(httpClient).encoder(encoder()).decoder(decoder()).logger(new Logger() {

            @Override
            protected void log(String configKey, String format, Object... args) {
                log.info("{} {}", configKey, String.format(format, args));

            }
        }).logLevel(Level.BASIC)
            .requestInterceptor(authRequestInterceptor)
            .target(CromwellClient.class, cromwellConfig.getUrl());
    }

    @Bean
    public WdlValidatorClient wdlValidatorClient(WdlValidatorClientConfig validatorConfig) {
        Client httpClient = new OkHttpClient();

        return Feign.builder().client(httpClient).encoder(encoder()).decoder(decoder()).logger(new Logger() {

            @Override
            protected void log(String configKey, String format, Object... args) {
                log.info("{} {}", configKey, String.format(format, args));

            }
        }).logLevel(Level.BASIC)
            .target(WdlValidatorClient.class, validatorConfig.getUrl());
    }


}
