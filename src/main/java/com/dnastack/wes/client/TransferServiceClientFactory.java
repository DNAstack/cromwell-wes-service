package com.dnastack.wes.client;

import com.dnastack.wes.client.ClientConfigurations.SimpleLogger;
import com.dnastack.wes.config.AuthConfig;
import com.dnastack.wes.config.TransferConfig;
import com.dnastack.wes.exception.TransferServiceDisabledException;
import com.dnastack.wes.model.oauth.AccessToken;
import feign.Client;
import feign.Feign;
import feign.Logger.Level;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.jackson.JacksonEncoder;
import feign.okhttp.OkHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TransferServiceClientFactory {

    private final AuthConfig authConfig;
    private final TransferConfig transferConfig;
    private final Encoder encoder;
    private final Decoder decoder;

    @Autowired
    public TransferServiceClientFactory(AuthConfig authConfig, TransferConfig transferConfig, Encoder encoder, Decoder decoder) {
        this.authConfig = authConfig;
        this.transferConfig = transferConfig;
        this.encoder = encoder;
        this.decoder = decoder;
    }

    private TransferServiceClient instance;

    public TransferServiceClient getClient() {
        if (transferConfig.isEnabled()) {
            if (instance == null) {
                OauthTokenClient oauthTokenClient = Feign.builder().client(new OkHttpClient()).encoder(encoder)
                    .decoder(decoder).logger(new SimpleLogger())
                    .logLevel(Level.BASIC)
                    .target(OauthTokenClient.class, authConfig.getServiceAccountAuthenticationUri());
                OAuthTokenCache tokenCache = new OAuthTokenCache(oauthTokenClient, authConfig);
                Client httpClient = new OkHttpClient();
                instance = Feign.builder().client(httpClient).encoder(new JacksonEncoder()).decoder(decoder)
                    .logger(new SimpleLogger())
                    .logLevel(Level.BASIC)
                    .requestInterceptor((template) -> {
                        AccessToken accessToken = tokenCache.getToken(transferConfig.getObjectTransferUri());
                        template.header("Authorization", "Bearer " + accessToken.getToken());
                    })
                    .target(TransferServiceClient.class, transferConfig.getObjectTransferUri());
            }
            return instance;
        } else {
            throw new TransferServiceDisabledException("Cannot retrieve a client for the transfer service, transfers are disabled");
        }

    }

}
