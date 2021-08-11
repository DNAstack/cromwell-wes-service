package com.dnastack.wes.shared;

import com.dnastack.wes.security.AuthConfig;
import feign.FeignException;

import java.time.Instant;


/**
 * This class provides a simple cache to retrieve an instance of the service account access token. This access token
 * should be used for Service -> Service communications instead of the supplied user access token.
 */

public class OAuthTokenCache {

    private static final Long TOKEN_BUFFER = 30L;

    private final OauthTokenClient tokenClient;

    private final AuthConfig authConfig;
    private AccessToken token;
    private Long issuedAt = 0L;

    public OAuthTokenCache(OauthTokenClient tokenClient, AuthConfig authConfig) {
        this.tokenClient = tokenClient;
        this.authConfig = authConfig;
    }

    /**
     * Retrieve a token in a thread safe way. If the token exists, make sure the token is not expired within a buffer
     * zone. If the the token has already expired, then retrieve a new token.
     *
     * @param audience
     */
    public synchronized AccessToken getToken(String audience) {
        if (token == null) {
            token = retrieveToken(audience);
        } else if (token.getExpiresIn() + issuedAt < (Instant.now().getEpochSecond() - TOKEN_BUFFER)) {
            token = retrieveToken(audience);
        }

        return token.clone();

    }

    private AccessToken retrieveToken(String audience) {
        OAuthRequest request = new OAuthRequest();
        request.setClientId(authConfig.getServiceAccountClientId());
        request.setClientSecret(authConfig.getServiceAccountSecret());
        request.setGrantType("client_credentials");
        request.setAudience(audience);
        try {
            AccessToken accessToken = tokenClient.getToken(request);
            issuedAt = Instant.now().getEpochSecond();

            return accessToken;
        } catch (FeignException fe) {
            throw new ServiceAccountException(fe);
        }
    }

}
