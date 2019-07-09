package com.dnastack.wes.client;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties("app.wdl.validator")
public class WdlValidatorClientConfig {

    String url = "https://wdl-validator.staging.dnastack.com";
}
