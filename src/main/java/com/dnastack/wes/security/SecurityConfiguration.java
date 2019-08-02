package com.dnastack.wes.security;

import com.dnastack.wes.config.AuthConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.JwsAlgorithms;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoderJwkSupport;

@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
@Slf4j
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {


    @Bean
    public JwtDecoder jwtDecoder(AuthConfig authConfig) {
        OAuth2TokenValidator<Jwt> jwtValidator =
            JwtValidators.createDefaultWithIssuer(authConfig.getIssuerUri());

        if (authConfig.getRsaPublicKey() != null) {
            PublicKeyJwtDecoder publicKeyJwtDecoder = new PublicKeyJwtDecoder(authConfig.getRsaPublicKey());
            publicKeyJwtDecoder.setJwtValidator(jwtValidator);
            return publicKeyJwtDecoder;
        } else if (authConfig.getJwkSetUri() != null) {
            NimbusJwtDecoderJwkSupport nimbusJwtDecoderJwkSupport =
                new NimbusJwtDecoderJwkSupport(authConfig.getJwkSetUri());
            nimbusJwtDecoderJwkSupport.setJwtValidator(jwtValidator);
            return nimbusJwtDecoderJwkSupport;
        } else {
            return JwtDecoders.fromOidcIssuerLocation(authConfig.getIssuerUri());
        }
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .authorizeRequests()
            .antMatchers("/actuator", "/actuator/info", "/actuator/health").permitAll()
            .antMatchers("/").permitAll()
            .antMatchers("/index.html").permitAll()
            .antMatchers("/ga4gh/drs/**").permitAll()
            .antMatchers("/ga4gh/wes/v1/service-info").permitAll()
            .anyRequest().authenticated()
            .and()
            .oauth2ResourceServer()
            .jwt();
    }


}
