package com.dnastack.wes.security;


import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

@Slf4j
public class AuthenticatedUser {

    public static Jwt getJwt(){
        try {
            return (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        } catch (Exception e) {
            log.warn(e.getMessage());
            return null;
        }
    }

    public static String getSubject() {
        try {
            return ((Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getSubject();
        } catch (Exception e) {
            log.warn(e.getMessage());
            return null;
        }
    }

}
