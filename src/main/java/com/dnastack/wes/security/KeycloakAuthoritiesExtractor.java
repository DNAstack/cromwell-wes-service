package com.dnastack.wes.security;

import com.dnastack.wes.AuthConfig;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.stereotype.Component;

@Component("keycloak")
@Slf4j
public class KeycloakAuthoritiesExtractor extends JwtAuthenticationConverter {

    // Maps keycloak roles to a list of scopes
    private Map<String, List<String>> roleMapping;
    private String clientId;

    @Autowired
    public KeycloakAuthoritiesExtractor(AuthConfig authConfig) {
        this.roleMapping = authConfig.getRoleMapping();
        this.clientId = authConfig.getClientId();
    }

    private Stream<GrantedAuthority> translateKeycloakRole(String keycloakRole) {
        List<String> authorities = roleMapping.get(keycloakRole);
        if (authorities != null) {
            return Stream.concat(Stream.of(new SimpleGrantedAuthority("ROLE_" + keycloakRole)), authorities.stream().
                map(authority -> new SimpleGrantedAuthority("SCOPE_" + authority)));

        }
        return Stream.empty();
    }

    private Stream<GrantedAuthority> getAuthoritiesFromRealmRoles(Jwt jwt) {
        Map<String, List<String>> m = (Map) jwt.getClaims().get("realm_access");
        if (m != null) {
            List<String> keycloakRoles = m.get("roles");
            if (keycloakRoles != null) {
                return keycloakRoles.stream().flatMap(this::translateKeycloakRole);
            }
        }
        return Stream.empty();
    }

    private Stream<GrantedAuthority> getAuthoritiesFromResourceRoles(Jwt jwt) {
        Map<String, Map<String, List<String>>> m = (Map) jwt.getClaims().get("resource_access");
        if (m != null) {
            Map<String, List<String>> m2 = m.get(clientId);
            if (m2 != null) {
                List<String> keycloakRoles = m2.get("roles");
                if (keycloakRoles != null) {
                    return keycloakRoles.stream().flatMap(this::translateKeycloakRole);
                }
            }
        }
        return Stream.empty();
    }

    protected Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        return Stream.concat(getAuthoritiesFromRealmRoles(jwt), getAuthoritiesFromResourceRoles(jwt))
            .map((grantedAuthority) -> {
                log.debug("Got granted authority: " + grantedAuthority.getAuthority());
                return grantedAuthority;
            })
            .collect(Collectors.toSet());
    }

}