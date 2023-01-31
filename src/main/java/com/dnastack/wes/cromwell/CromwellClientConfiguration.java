package com.dnastack.wes.cromwell;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Client;
import feign.Feign;
import feign.Logger;
import feign.auth.BasicAuthRequestInterceptor;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.form.FormEncoder;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import feign.okhttp.OkHttpClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class CromwellClientConfiguration {
    @Bean
    public Encoder encoder(ObjectMapper mapper) {
        return new FormEncoder(new JacksonEncoder(mapper));
    }

    @Bean
    public Decoder decoder(ObjectMapper mapper) {
        return new JacksonDecoder(mapper);
    }
    @Bean
    public CromwellClient cromwellClient(Encoder encoder, Decoder decoder, CromwellConfig cromwellConfig) {
        Client httpClient = new OkHttpClient();

        Feign.Builder builder = Feign.builder().client(httpClient).encoder(encoder).decoder(decoder)
            .logger(new SimpleLogger()).logLevel(Logger.Level.BASIC);
        if (cromwellConfig.getPassword() != null && cromwellConfig.getUsername() != null) {
            builder.requestInterceptor(new BasicAuthRequestInterceptor(cromwellConfig
                .getUsername(), cromwellConfig.getPassword()));
        }

        return builder.target(CromwellClient.class, cromwellConfig.getUrl());

    }

    public static class SimpleLogger extends Logger {

        @Override
        protected void log(String configKey, String format, Object... args) {
            log.info("{} {}", configKey, String.format(format, args));

        }

    }


}
