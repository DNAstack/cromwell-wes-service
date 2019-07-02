package com.dnastack.wes.security;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.keycloak")
@Getter
@Setter
public class KeycloakConfiguration {
    private String clientId;
    private Map<String, List<String>> roleMapping = new HashMap<>();
}
