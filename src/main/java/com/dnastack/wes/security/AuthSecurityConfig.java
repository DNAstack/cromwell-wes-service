package com.dnastack.wes.security;

import brave.Tracing;
import com.dnastack.auth.JwtTokenParser;
import com.dnastack.auth.JwtTokenParserFactory;
import com.dnastack.auth.PermissionChecker;
import com.dnastack.auth.PermissionCheckerFactory;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

@ConditionalOnProperty(
    prefix = "security",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true
)
@Slf4j
@EnableWebSecurity
@EnableMethodSecurity
@Configuration
public class AuthSecurityConfig {

    @Bean
    // purposefully uses same qualifier as bean in spring-wallet-token-validator library in case we ever start using
    // that library in Explorer
    @Qualifier("com.dnastack.auth.token-validator-connection-pool")
    public ConnectionPool getConnectionPool(AuthConfig authConfig) {
        final AuthConfig.HttpClientConfig httpClientConfig = authConfig.getHttpClientConfig();
        final Duration keepAliveTimeout = httpClientConfig.getKeepAliveTimeout();
        if (keepAliveTimeout.toNanosPart() > 0 || keepAliveTimeout.toMillisPart() > 0) {
            throw new IllegalArgumentException("Given keep alive value of [%s] must not have granularity below seconds".formatted(keepAliveTimeout));
        }
        final long convertedTimeout = keepAliveTimeout.toSeconds();
        final TimeUnit convertedTimeUnit = TimeUnit.SECONDS;

        return new ConnectionPool(httpClientConfig.getMaxIdleConnections(), convertedTimeout, convertedTimeUnit);
    }

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
    public List<IssuerInfo> allowedIssuers(
        AuthConfig authConfig,
        Tracing tracing,
        @Qualifier("com.dnastack.auth.token-validator-connection-pool") ConnectionPool connectionPool
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
        List<IssuerInfo> allowedIssuers,
        Tracing tracing,
        @Qualifier("com.dnastack.auth.token-validator-connection-pool") ConnectionPool connectionPool
    ) {
        final TokenActionsHttpClient tokenActionsHttpClient = TokenActionsHttpClientFactory.create(tracing, connectionPool);
        return JwtTokenParserFactory.create(allowedIssuers, tokenActionsHttpClient);
    }

    @Bean
    public PermissionChecker permissionChecker(
        List<IssuerInfo> allowedIssuers,
        @Value("${wes.auth.validator.policy-evaluation-requester}") String policyEvaluationRequester,
        @Value("${wes.auth.validator.policy-evaluation-uri}") String policyEvaluationUri,
        Tracing tracing,
        @Qualifier("com.dnastack.auth.token-validator-connection-pool") ConnectionPool connectionPool
    ) {
        return PermissionCheckerFactory.create(allowedIssuers, policyEvaluationRequester, policyEvaluationUri, tracing, connectionPool);
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
