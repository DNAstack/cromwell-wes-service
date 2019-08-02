package com.dnastack.wes.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties("wes.wdl.validator")
public class WdlValidatorClientConfig {

    /**
     * URL to a WDL Validation API. The WDL validator is used to first validate the workflow, and then provide a
     * machine readable version of the inputs to use
     */
    String url = null;
}
