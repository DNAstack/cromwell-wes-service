package com.dnastack.wes.security;


import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

public class AuthenticatedUser {

    /**
     * @return The structured representation of the Jwt.
     */
    public static Jwt getJwt() {
        return (Jwt) (SecurityContextHolder.getContext().getAuthentication().getCredentials());
    }

    /**
     * @return The token value of the JWT.
     */
    public static String getBearerToken() {
        return getJwt().getTokenValue();
    }

    public static String getSubject() {
        return getJwt().getSubject();
    }

}
