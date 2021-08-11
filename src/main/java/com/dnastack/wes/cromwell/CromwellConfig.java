package com.dnastack.wes.cromwell;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties("wes.cromwell")
public class CromwellConfig {

    /**
     * The root url of the cromwell API.
     */
    String url = "http://localhost:8000";

    /**
     * Provide a username to use for basic Auth
     */
    String username = null;

    /**
     * Provide a password to use for basic Auth
     */
    String password = null;

}
