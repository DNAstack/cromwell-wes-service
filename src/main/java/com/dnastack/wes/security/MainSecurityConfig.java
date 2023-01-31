package com.dnastack.wes.security;

import brave.Tracing;
import com.dnastack.auth.JwtTokenParser;
import com.dnastack.auth.JwtTokenParserFactory;
import com.dnastack.auth.PermissionChecker;
import com.dnastack.auth.PermissionCheckerFactory;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;

@Slf4j
@EnableWebSecurity
@EnableMethodSecurity
@Configuration
public class MainSecurityConfig {

    @Bean
    protected SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.sessionManagement()
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .cors().disable()
            .csrf().disable()
            .authorizeRequests()
            .antMatchers("/services", "/actuator/info**", "/actuator/health", "/actuator/health/**", "/service-info", "/docs/**")
            .permitAll()
            .antMatchers("/ga4gh/wes/v1/service-info")
            .permitAll()
            .antMatchers("/actuator/**").authenticated()
            .antMatchers("/**")
            .authenticated()
            .and()
            .oauth2ResourceServer()
            .jwt();
        return http.build();
    }

    @Bean
    public List<IssuerInfo> allowedIssuers(AuthConfig authConfig) {
        final AuthConfig.IssuerConfig issuerConfig = authConfig.getTokenIssuer();
        final String issuerUri = issuerConfig.getIssuerUri();
        return List.of(IssuerInfo.IssuerInfoBuilder.builder()
            .issuerUri(issuerUri)
            .allowedAudiences(issuerConfig.getAudiences())
            .allowedResources(issuerConfig.getResources())
            .publicKeyResolver(issuerConfig.getRsaPublicKey() != null
                ? new IssuerPubKeyStaticResolver(issuerUri, issuerConfig.getRsaPublicKey())
                : new CachingIssuerPubKeyJwksResolver(issuerUri))
            .build());
    }

    @Bean
    public JwtTokenParser tokenParser(List<IssuerInfo> allowedIssuers, Tracing tracing) {
        final TokenActionsHttpClient tokenActionsHttpClient = TokenActionsHttpClientFactory.create(tracing);
        return JwtTokenParserFactory.create(allowedIssuers, tokenActionsHttpClient);
    }

    @Bean
    public PermissionChecker permissionChecker(
        List<IssuerInfo> allowedIssuers,
        @Value("${wes.auth.validator.policy-evaluation-requester}") String policyEvaluationRequester,
        @Value("${wes.auth.validator.policy-evaluation-uri}") String policyEvaluationUri,
        Tracing tracing
    ) {
        return PermissionCheckerFactory.create(allowedIssuers, policyEvaluationRequester, policyEvaluationUri, tracing);
    }


    @Bean
    public JwtDecoder jwtDecoder(JwtTokenParser jwtTokenParser, PermissionChecker permissionChecker) {
        return jwtToken -> {
            try {
                permissionChecker.checkPermissions(jwtToken);
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
