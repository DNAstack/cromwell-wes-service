package com.dnastack.wes.security;

import brave.Tracing;
import com.dnastack.auth.JwtTokenParser;
import com.dnastack.auth.PermissionChecker;
import com.dnastack.auth.PermissionCheckerFactory;
import com.dnastack.auth.model.IssuerInfo;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.JwtException;
import okhttp3.ConnectionPool;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Configure API level Authorization enforcing the <code>@PreAuthorize</code> annotations for all controller methods. Permissions
 * are granted through the use of DNAstack Wallet
 */
@Configuration
@EnableMethodSecurity
@ConditionalOnExpression("${security.authentication.enabled:true} && ${securtiy.authorization.enabled:true}")
public class AuthorizedSecurityConfiguration {

    @Bean
    public AccessEvaluator accessEvaluator(AuthConfig authConfig, PermissionChecker permissionChecker) {
        return new AccessEvaluator(authConfig, permissionChecker);
    }

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
