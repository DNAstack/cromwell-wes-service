package com.dnastack.wes.security;


import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.AbstractOAuth2Token;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@Slf4j
public class AuthenticatedUser {

    public static String getSubject() {
        try {
            Object cred = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (cred instanceof Jwt) {
                return ((Jwt) cred).getSubject();
            } else if (cred instanceof OidcUser) {
                return ((OidcUser) cred).getName();
            } else {
                return null;
            }
        } catch (Exception e) {
            log.warn(e.getMessage());
            return null;
        }
    }

}
