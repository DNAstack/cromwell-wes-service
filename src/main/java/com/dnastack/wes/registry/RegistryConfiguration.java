package com.dnastack.wes.registry;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Data
@Configuration
@ConfigurationProperties("wes.registry")
public class RegistryConfiguration {

    Map<String, Ga4ghService> services;

}
