package com.dnastack.wes.cromwell;

import com.dnastack.wes.shared.ClientConfigurations;
import feign.Client;
import feign.Feign;
import feign.Logger;
import feign.auth.BasicAuthRequestInterceptor;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.okhttp.OkHttpClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class CromwellClientConfiguration {

    @Bean
    public CromwellClient cromwellClient(Encoder encoder, Decoder decoder, CromwellConfig cromwellConfig) {
        Client httpClient = new OkHttpClient();

        Feign.Builder builder = Feign.builder().client(httpClient).encoder(encoder).decoder(decoder)
            .logger(new ClientConfigurations.SimpleLogger()).logLevel(Logger.Level.BASIC);
        if (cromwellConfig.getPassword() != null && cromwellConfig.getUsername() != null) {
            builder.requestInterceptor(new BasicAuthRequestInterceptor(cromwellConfig
                .getUsername(), cromwellConfig.getPassword()));
        }

        return builder.target(CromwellClient.class, cromwellConfig.getUrl());

    }

}
