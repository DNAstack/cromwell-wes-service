package com.dnastack.wes.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dnastack.auth.PermissionChecker;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

class AccessEvaluatorTest {

    private static final String AUDIENCE = "http://localhost:8090";

    private final PermissionChecker permissionChecker = mock(PermissionChecker.class);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private AccessEvaluator evaluator() {
        AuthConfig authConfig = new AuthConfig();
        AuthConfig.IssuerConfig issuerConfig = new AuthConfig.IssuerConfig();
        issuerConfig.setAudiences(List.of(AUDIENCE));
        authConfig.tokenIssuer = issuerConfig;
        return new AccessEvaluator(authConfig, permissionChecker);
    }

    @Test
    void canAccessResource_forwardsRequiredActionsVerbatimToPermissionChecker() {
        givenAuthenticatedJwt();
        ArgumentCaptor<Set<String>> forwarded = captureForwardedActions();

        evaluator().canAccessResource("/ga4gh/wes/v1/runs", Set.of("workbench.runs.list"), Set.of("wes"));

        verify(permissionChecker).hasPermissions(eq("token-value"), any(), eq(AUDIENCE + "/ga4gh/wes/v1/runs"), forwarded.capture());
        assertThat(forwarded.getValue()).containsExactly("workbench.runs.list");
    }

    private void givenAuthenticatedJwt() {
        Jwt jwt = Jwt.withTokenValue("token-value")
            .header("alg", "none")
            .claim("sub", "test-user")
            .build();
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(jwt, null));
    }

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<Set<String>> captureForwardedActions() {
        when(permissionChecker.hasPermissions(any(), any(), any(), any())).thenReturn(true);
        return ArgumentCaptor.forClass(Set.class);
    }
}
