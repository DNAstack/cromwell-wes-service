package com.dnastack.wes.security;


import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

public class AuthenticatedUser {

    /**
     * @return The structured representation of the Jwt.
     */
    public static Jwt getJwt() {
        try {
            return (Jwt) (SecurityContextHolder.getContext().getAuthentication().getCredentials());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * @return The token value of the JWT.
     */
    public static String getBearerToken() {
        try {
            return getJwt().getTokenValue();
        } catch (Exception e) {
            return null;
        }
    }

    public static String getSubject() {
        try {
            return getJwt().getSubject();
        } catch (Exception e) {
            return null;
        }
    }

}
