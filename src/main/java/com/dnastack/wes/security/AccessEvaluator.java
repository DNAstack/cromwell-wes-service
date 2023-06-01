package com.dnastack.wes.security;

import com.dnastack.auth.PermissionChecker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
public class AccessEvaluator {

    private final List<String> audiences;
    private final PermissionChecker permissionChecker;

    public AccessEvaluator(AuthConfig authConfig, PermissionChecker permissionChecker) {
        this.audiences = authConfig.tokenIssuer.getAudiences();
        this.permissionChecker = permissionChecker;
    }

    /**
     * Usage of this method:
     *
     * @param requiredResource path to the api endpoint
     * @param requiredActions  check actions defined in policy
     * @param requiredScopes   check scopes defined in policy
     *
     * @return boolean value specifying whether the user can access the resource
     *
     * @PreAuthorize("@accessEvaluator.canAccessResource('/api/endpoint', 'app:feature:read', 'openid')")
     * Add the above line with appropriate api endpoint, actions and scopes on a controller method
     * to preauthorize the request
     * Additionally, you can handle exceptions using @ExceptionHandler
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
            .map(Jwt.class::cast)
            .map(jwtPrincipal -> {
                boolean hasPermissions = audiences.stream().anyMatch(audience -> {
                    final String fullResourceUrl = audience + requiredResource;
                    return permissionChecker.hasPermissions(jwtPrincipal.getTokenValue(), requiredScopes, fullResourceUrl, requiredActions);
                });
                if (!hasPermissions) {
                    log.info("Denying access to {} for {}. allowedAudiences={}; requiredScopes={}; requiredActions={}; actualScopes={}; actualActions={}",
                        jwtPrincipal.getSubject(), requiredResource, audiences, requiredScopes, requiredActions,
                        jwtPrincipal.getClaims().get("scope"), jwtPrincipal.getClaims().get("actions"));
                }
                return hasPermissions;
            })
            .orElse(false);
    }

}
