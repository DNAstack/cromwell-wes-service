package com.dnastack.wes.security;

import brave.Tracing;
import com.dnastack.auth.JwtTokenParser;
import com.dnastack.auth.JwtTokenParserFactory;
import com.dnastack.auth.PermissionChecker;
import com.dnastack.auth.PermissionCheckerFactory;
import com.dnastack.auth.client.TokenActionsClient;
import com.dnastack.auth.client.TokenActionsHttpClientFactory;
import com.dnastack.auth.keyresolver.CachingIssuerPubKeyJwksResolver;
import com.dnastack.auth.keyresolver.IssuerPubKeyStaticResolver;
import com.dnastack.auth.model.IssuerInfo;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwsHeader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
import java.util.stream.Collectors;

// Taken almost verbatim from other security configurations such as
// https://github.com/DNAstack/wes-service/blob/master/src/main/java/com/dnastack/wes/security/SecurityConfiguration.java
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
@Configuration
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.sessionManagement()
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .cors()
            .and()
            .csrf().disable()
            .authorizeRequests()
            .antMatchers("/actuator", "/actuator/info", "/actuator/health").permitAll()
            .antMatchers("/").permitAll()
            .antMatchers("/index.html").permitAll()
            .antMatchers("/ga4gh/drs/**").permitAll()
            .antMatchers("/ga4gh/wes/v1/service-info").permitAll()
            .anyRequest()
            .authenticated()
            .and()
            .oauth2ResourceServer()
            .jwt();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(@Value("${app.cors.urls:localhost:8090}") List<String> cors) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(cors);
        configuration.setAllowedMethods(cors);
        configuration.setAllowedHeaders(cors);
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public List<IssuerInfo> allowedIssuers(AuthConfig authConfig) {
        List<AuthConfig.IssuerConfig> issuers = authConfig.getTokenIssuers();
        if (issuers == null || issuers.isEmpty()) {
            throw new IllegalArgumentException("At least one token issuer must be defined");
        }

        return authConfig.getTokenIssuers().stream()
            .map((issuerConfig) -> {
                final String issuerUri = issuerConfig.getIssuerUri();
                return IssuerInfo.IssuerInfoBuilder.builder()
                    .issuerUri(issuerUri)
                    .allowedAudiences(issuerConfig.getAudiences())
                    .publicKeyResolver(issuerConfig.getRsaPublicKey() != null
                        ? new IssuerPubKeyStaticResolver(issuerUri, issuerConfig.getRsaPublicKey())
                        : new CachingIssuerPubKeyJwksResolver(issuerUri))
                    .build();
            })
            .collect(Collectors.toUnmodifiableList());
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
    public JwtTokenParser tokenParser(List<IssuerInfo> allowedIssuers, Tracing tracing){
       return JwtTokenParserFactory.create(allowedIssuers, TokenActionsHttpClientFactory.create(tracing));
    }

    @Bean
    public JwtDecoder jwtDecoder(PermissionChecker permissionChecker, JwtTokenParser jwtTokenParser) {
        return (jwtToken) -> {
            permissionChecker.checkPermissions(jwtToken);
            final Jws<Claims> jws = jwtTokenParser.parseTokenClaims(jwtToken);
            final JwsHeader headers = jws.getHeader();
            final Claims claims = jws.getBody();
            return new Jwt(jwtToken, claims.getIssuedAt().toInstant(), claims.getExpiration().toInstant(), headers, claims);
        };
    }

}
