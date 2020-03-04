package com.dnastack.wes.security;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.Assert;

public class JwtScopeValidator implements OAuth2TokenValidator<Jwt> {

    private final List<String> requiredScopes;
    private static final String SCOPE_CLAIM_NAME = "scope";

    public JwtScopeValidator(List<String> requiredScopes) {
        this.requiredScopes = requiredScopes;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        Assert.notNull(jwt, "jwt cannot be null");
        Object tokenScope = jwt.getClaims().get(SCOPE_CLAIM_NAME);

        if (tokenScope != null) {
            Collection scopeCollection;
            if (tokenScope instanceof String) {
                scopeCollection = Arrays.asList(((String) tokenScope).split(" "));
            } else if (tokenScope instanceof Collection) {
                scopeCollection = (Collection) tokenScope;
            } else {
                scopeCollection = Collections.singletonList(tokenScope);
            }

            if (!scopeCollection.containsAll(requiredScopes)) {
                OAuth2Error error = new OAuth2Error(
                    OAuth2ErrorCodes.INVALID_REQUEST,
                    String.format("Jwt has invalid audience. Expected audience to contain: \"%s\"", requiredScopes),
                    "https://tools.ietf.org/html/rfc6750#section-3.1"
                );
                return OAuth2TokenValidatorResult.failure(error);
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