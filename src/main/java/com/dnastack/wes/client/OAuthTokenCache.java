package com.dnastack.wes.client;

import com.dnastack.wes.config.AuthConfig;
import com.dnastack.wes.model.oauth.AccessToken;
import com.dnastack.wes.model.oauth.OAuthRequest;
import java.time.Instant;


/**
 * This class provides a simple cache to retrieve an instance of the service account access token. This access token
 * should be used for Service -> Service communications instead of the supplied user access token.
 */

public class OAuthTokenCache {

    private static final Long TOKEN_BUFFER = 30L;

    private final OauthTokenClient tokenClient;

    private final AuthConfig authConfig;

    public OAuthTokenCache(OauthTokenClient tokenClient, AuthConfig authConfig) {
        this.tokenClient = tokenClient;
        this.authConfig = authConfig;
    }

    private AccessToken token;

    private Long issuedAt = 0L;


    /**
     * Retrieve a token in a thread safe way. If the token exists, make sure the token is not expired within a buffer
     * zone. If the the token has already expired, then retrieve a new token.
     */
    public synchronized AccessToken getToken() {
        if (token == null) {
            token = retrieveToken();
        } else if (token.getExpiresIn() + issuedAt < (Instant.now().getEpochSecond() - TOKEN_BUFFER)) {
            token = retrieveToken();
        }

        return token.clone();

    }

    private AccessToken retrieveToken() {
        OAuthRequest request = new OAuthRequest();
        request.setClientId(authConfig.getServiceAccountClientId());
        request.setClientSecret(authConfig.getServiceAccountSecret());
        request.setGrantType("client_credentials");
        AccessToken accessToken = tokenClient.getToken(request);
        issuedAt = Instant.now().getEpochSecond();
        return accessToken;
    }
}
