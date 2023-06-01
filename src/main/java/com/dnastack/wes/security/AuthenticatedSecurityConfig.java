package com.dnastack.wes.security;

import brave.Tracing;
import com.dnastack.auth.JwtTokenParser;
import com.dnastack.auth.JwtTokenParserFactory;
import com.dnastack.auth.client.OidcHttpClient;
import com.dnastack.auth.client.TokenActionsHttpClient;
import com.dnastack.auth.client.TokenActionsHttpClientFactory;
import com.dnastack.auth.keyresolver.CachingIssuerPubKeyJwksResolver;
import com.dnastack.auth.keyresolver.IssuerPubKeyStaticResolver;
import com.dnastack.auth.model.IssuerInfo;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ConnectionPool;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;

/**
 * Configure JWT Authentication for the WES server
 */
@Slf4j
@EnableWebSecurity
@Configuration
@ConditionalOnExpression("${security.authentication.enabled:true}")
public class AuthenticatedSecurityConfig {


    @Bean
    protected SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.sessionManagement()
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .cors()
            .disable()
            .csrf()
            .disable()
            .authorizeRequests()
            .antMatchers("/services", "/actuator/info**", "/actuator/health", "/actuator/health/**", "/service-info", "/docs/**")
            .permitAll()
            .antMatchers("/ga4gh/wes/v1/service-info")
            .permitAll()
            .antMatchers("/actuator/**")
            .authenticated()
            .antMatchers("/**")
            .authenticated()
            .and()
            .oauth2ResourceServer()
            .jwt();
        return http.build();
    }

    @Bean
    public List<IssuerInfo> allowedIssuers(
        AuthConfig authConfig, Tracing tracing, @Qualifier("com.dnastack.auth.token-validator-connection-pool") ConnectionPool connectionPool
    ) {
        final AuthConfig.IssuerConfig issuerConfig = authConfig.getTokenIssuer();
        final String issuerUri = issuerConfig.getIssuerUri();
        return List.of(IssuerInfo.IssuerInfoBuilder.builder()
            .issuerUri(issuerUri)
            .allowedAudiences(issuerConfig.getAudiences())
            .allowedResources(issuerConfig.getResources())
            .publicKeyResolver(issuerConfig.getRsaPublicKey() != null
                ? new IssuerPubKeyStaticResolver(issuerUri, issuerConfig.getRsaPublicKey())
                : CachingIssuerPubKeyJwksResolver.create(issuerUri, new OidcHttpClient(tracing, connectionPool)))
            .build());
    }

    @Bean
    public JwtTokenParser tokenParser(
        List<IssuerInfo> allowedIssuers, Tracing tracing, @Qualifier("com.dnastack.auth.token-validator-connection-pool") ConnectionPool connectionPool
    ) {
        final TokenActionsHttpClient tokenActionsHttpClient = TokenActionsHttpClientFactory.create(tracing, connectionPool);
        return JwtTokenParserFactory.create(allowedIssuers, tokenActionsHttpClient);
    }


    @Bean
    @ConditionalOnExpression("!${security.authoriation.enabled:true}")
    public JwtDecoder jwtDecoder(JwtTokenParser jwtTokenParser) {
        return jwtToken -> {
            try {
                final Jws<Claims> jws = jwtTokenParser.parseTokenClaims(jwtToken);
                final JwsHeader<?> headers = jws.getHeader();
                final Claims claims = jws.getBody();
                return new Jwt(jwtToken, claims.getIssuedAt().toInstant(), claims.getExpiration().toInstant(), headers, claims);
            } catch (JwtException e) {
                throw new org.springframework.security.oauth2.jwt.BadJwtException(e.getMessage(), e);
            }
        };
    }


}
