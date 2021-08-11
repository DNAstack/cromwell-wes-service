package com.dnastack.wes.shared;


import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Logger;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.form.FormEncoder;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
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

    @Bean
    public Encoder encoder(ObjectMapper mapper) {
        return new FormEncoder(new JacksonEncoder(mapper));
    }

    @Bean
    public Decoder decoder(ObjectMapper mapper) {
        return new JacksonDecoder(mapper);
    }


    public static class SimpleLogger extends Logger {

        @Override
        protected void log(String configKey, String format, Object... args) {
            log.info("{} {}", configKey, String.format(format, args));

        }

    }

}
