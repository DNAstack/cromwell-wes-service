package com.dnastack.wes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;


@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.auth")
public class AuthConfig {


    String identityProvider;

    String oidcTokenUri;

    String serviceAccountClientId;

    String serviceAccountSecret;

    private String clientId;

    private Map<String, List<String>> roleMapping = new HashMap<>();

}
