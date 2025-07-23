package com.dnastack.wes.cromwell;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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


    /**
     * Default workflow options to apply at runtime
     */
    Map<String, Object> defaultWorkflowOptions = new HashMap<>();

    String userLabel = "user_id";

    String workflowUrlLabel = "workflow_url";

    String optionsFilename = "options.json";

    String dependenciesFilename = "dependencies.zip";

    List<String> filesToIgnoreForStaging = List.of();

    List<String> validCromwellOptions = List.of();


}
