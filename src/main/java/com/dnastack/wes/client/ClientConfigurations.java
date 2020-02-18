package com.dnastack.wes.client;


import com.dnastack.wes.config.CromwellConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Client;
import feign.Feign;
import feign.Feign.Builder;
import feign.Logger;
import feign.Logger.Level;
import feign.auth.BasicAuthRequestInterceptor;
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


    public static class SimpleLogger extends Logger {

        @Override
        protected void log(String configKey, String format, Object... args) {
            log.info("{} {}", configKey, String.format(format, args));

        }
    }

    @Bean
    public CromwellClient cromwellClient(CromwellConfig cromwellConfig) {
        Client httpClient = new OkHttpClient();

        Builder builder = Feign.builder().client(httpClient).encoder(encoder()).decoder(decoder())
            .logger(new SimpleLogger()).logLevel(Level.BASIC);
        if (cromwellConfig.getPassword() != null && cromwellConfig.getUsername() != null) {
            builder.requestInterceptor(new BasicAuthRequestInterceptor(cromwellConfig
                .getUsername(), cromwellConfig.getPassword()));
        }

        return builder.target(CromwellClient.class, cromwellConfig.getUrl());

    }

}
