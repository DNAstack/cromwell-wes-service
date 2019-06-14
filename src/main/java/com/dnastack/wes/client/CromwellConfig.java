package com.dnastack.wes.client;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties("app.cromwell")
public class CromwellConfig {

    String url = "http://localhost:8000";

    String username;

    String password;

}
