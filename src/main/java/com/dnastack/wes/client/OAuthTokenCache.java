package com.dnastack.wes.client;

import com.dnastack.wes.AppConfig;
import com.dnastack.wes.OauthConfig;
import com.dnastack.wes.model.oauth.AccessToken;
import com.dnastack.wes.model.oauth.OAuthRequest;
import feign.FeignException;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OAuthTokenCache {

    private static final Long TOKEN_BUFFER = 30L;

    @Autowired
    private OauthTokenClient tokenClient;

    @Autowired
    private AppConfig appConfig;

    private AccessToken token;

    private Long issuedAt = 0L;

    public synchronized AccessToken getToken() {
        if (token == null) {
            token = retrieveToken();
        } else if (token.getExpiresIn() + issuedAt < (Instant.now().getEpochSecond() - TOKEN_BUFFER)) {
            token = retrieveToken();
        }

        return token.clone();

    }

    private AccessToken retrieveToken() {
        OauthConfig oAuthConfig = appConfig.getOauthConfig();
        OAuthRequest request = new OAuthRequest();
        request.setClientId(oAuthConfig.getServiceAccountClientId());
        request.setClientSecret(oAuthConfig.getServiceAccountSecret());
        request.setGrantType("client_credentials");
        try {
            AccessToken accessToken = tokenClient.getToken(request);
            issuedAt = Instant.now().getEpochSecond();
            return accessToken;
        } catch (FeignException e){
            log.error(e.contentUTF8());
            throw e;
        }
    }

}
