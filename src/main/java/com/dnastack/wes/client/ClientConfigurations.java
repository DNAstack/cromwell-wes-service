package com.dnastack.wes.client;


import com.dnastack.wes.AppConfig;
import com.dnastack.wes.model.oauth.AccessToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Client;
import feign.Feign;
import feign.Feign.Builder;
import feign.Logger;
import feign.Logger.Level;
import feign.Target;
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


    private class SimpleLogger extends Logger {

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

    @Bean
    public WdlValidatorClient wdlValidatorClient(WdlValidatorClientConfig validatorConfig) {
        Client httpClient = new OkHttpClient();

        return Feign.builder().client(httpClient).encoder(encoder()).decoder(decoder()).logger(new SimpleLogger())
            .logLevel(Level.BASIC)
            .target(WdlValidatorClient.class, validatorConfig.getUrl());
    }

    @Bean
    public DrsClient drsClient() {
        Client httpClient = new OkHttpClient();
        return Feign.builder().client(httpClient).encoder(encoder()).decoder(decoder()).logger(new SimpleLogger())
            .logLevel(Level.BASIC)
            .target(Target.EmptyTarget.create(DrsClient.class));
    }

    @Bean
    public ExternalAccountClient externalAccountClient(AppConfig appConfig) {
        Client httpClient = new OkHttpClient();
        return Feign.builder().client(httpClient).encoder(new JacksonEncoder(mapper)).decoder(decoder())
            .logger(new SimpleLogger())
            .logLevel(Level.BASIC)
            .target(ExternalAccountClient.class, appConfig.getTransferConfig().getExternalAccountUri());
    }

    @Bean
    public OauthTokenClient oauthTokenClient(AppConfig appConfig) {
        Client httpClient = new OkHttpClient();
        return Feign.builder().client(httpClient).encoder(encoder()).decoder(decoder()).logger(new SimpleLogger())
            .logLevel(Level.BASIC)
            .target(OauthTokenClient.class, appConfig.getOauthConfig().getOidcTokenUri());
    }

    @Bean
    public TransferServiceClient transferServiceClient(AppConfig appConfig, OAuthTokenCache tokenCache) {
        Client httpClient = new OkHttpClient();
        return Feign.builder().client(httpClient).encoder(new JacksonEncoder(mapper)).decoder(decoder()).logger(new SimpleLogger())
            .logLevel(Level.BASIC)
            .requestInterceptor((template) -> {
                AccessToken accessToken = tokenCache.getToken();
                template.header("Authorization", "Bearer " + accessToken.getToken());
            })
            .target(TransferServiceClient.class, appConfig.getTransferConfig().getObjectTransferUri());
    }

}
