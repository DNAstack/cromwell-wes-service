package com.dnastack.wes.v1.client;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties("app.wdl.validator")
public class WdlValidatorClientConfig {

    String url = "http://localhost:8080";
}
