package com.dnastack.wes.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.lang.Collections;
import java.util.Collection;
import java.util.List;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.Assert;

public class JwtAudienceValidator implements OAuth2TokenValidator<Jwt> {

    private final List<String> aud;

    public JwtAudienceValidator(List<String> audience) {
        this.aud = audience;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        Assert.notNull(jwt, "jwt cannot be null");
        Object tokenAudience = jwt.getClaims().get(Claims.AUDIENCE);
        if (tokenAudience != null) {
            if (tokenAudience instanceof Collection) {
                Collection audienceCollection = (Collection) tokenAudience;
                if (!Collections.containsAny(audienceCollection, aud)) {
                    OAuth2Error error = new OAuth2Error(
                        OAuth2ErrorCodes.INVALID_REQUEST,
                        String.format("Jwt has invalid audience. Expected audience to contain: \"%s\"", aud),
                        "https://tools.ietf.org/html/rfc6750#section-3.1"
                    );
                    return OAuth2TokenValidatorResult.failure(error);
                }
            } else {
                String tokenAudienceString = (String) tokenAudience;
                if (!aud.contains(tokenAudienceString)) {
                    OAuth2Error error = new OAuth2Error(
                        OAuth2ErrorCodes.INVALID_REQUEST,
                        String.format("Jwt has invalid audience. Expected audience to be: \"%s\"", aud),
                        "https://tools.ietf.org/html/rfc6750#section-3.1"
                    );
                    return OAuth2TokenValidatorResult.failure(error);
                }
            }

        } else {
            OAuth2Error error = new OAuth2Error(
                OAuth2ErrorCodes.INVALID_REQUEST,
                String.format("Jwt missing required claim \"aud\""),
                "https://tools.ietf.org/html/rfc6750#section-3.1"
            );
            return OAuth2TokenValidatorResult.failure(error);
        }

        return OAuth2TokenValidatorResult.success();
    }
}