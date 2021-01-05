package com.dnastack.wes.security;

import com.dnastack.auth.IssuerPubKeyJwksSupplier;
import com.dnastack.auth.PermissionChecker;
import com.dnastack.auth.PermissionCheckerFactory;
import com.dnastack.auth.model.IssuerInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.dnastack.wes.config.AuthConfig;

@Slf4j
@Component
public class AccessEvaluator {

    private final AuthConfig authConfig;
    private final String appUrl;

    @Autowired
    public AccessEvaluator(AuthConfig authConfig, @Value("${info.app.url}") String appUrl) {
        this.authConfig = authConfig;
        this.appUrl = appUrl;
    }

    /**
     * Usage of this method:
     * @PreAuthorize("@accessEvaluator.canAccessResource('/api/endpoint', 'app:feature:read', 'openid')")
     * Add the above line with appropriate api endpoint, actions and scopes on a controller method
     * to preauthorize the request
     * Additionally, you can handle exceptions using @ExceptionHandler
     * @param requiredResource path to the api endpoint
     * @param requiredActions check actions defined in policy
     * @param requiredScopes check scopes defined in policy
     * @return boolean value specifying whether the user can access the resource
     */
    public boolean canAccessResource(String requiredResource, Set<String> requiredActions, Set<String> requiredScopes) {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            log.warn("Authentication must be present in context, resolving access as denied.");
            return false;
        }
        if (!(authentication.getPrincipal() instanceof Jwt)) {
            log.warn("Principal must be type of {}, resolving access as denied.", Jwt.class.getName());
            return false;
        }
        return Optional.ofNullable(authentication.getPrincipal())
            .map((principal) -> (Jwt) principal)
            .map((jwtPrincipal) -> {
                final String fullResourceUrl = appUrl + requiredResource;
                final List<String> allowedAudiences = List.of(appUrl);
                final List<IssuerInfo> allowedIssuers = getAllowedIssuers();
                final PermissionChecker permissionChecker = PermissionCheckerFactory.create(allowedAudiences, allowedIssuers);
                return permissionChecker.hasPermissions(jwtPrincipal.getTokenValue(), requiredScopes, fullResourceUrl, requiredActions);
            })
            .orElse(false);
    }

    private List<IssuerInfo> getAllowedIssuers() {
        return authConfig.getTokenIssuers().stream()
            .map((issuerConfig) -> IssuerInfo.IssuerInfoBuilder.builder()
                .issuerUri(issuerConfig.getIssuerUri())
                .publicKeySupplier(new IssuerPubKeyJwksSupplier(issuerConfig.getIssuerUri()))
                .build())
            .collect(Collectors.toUnmodifiableList());
    }

}
