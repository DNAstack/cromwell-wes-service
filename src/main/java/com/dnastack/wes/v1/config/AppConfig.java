package com.dnastack.wes.v1.config;

import com.dnastack.wes.v1.model.wes.ServiceInfo;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties("app")
public class AppConfig {

    ServiceInfo serviceInfo = new ServiceInfo();

}
