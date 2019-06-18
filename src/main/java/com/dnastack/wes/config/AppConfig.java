package com.dnastack.wes.config;

import com.dnastack.wes.model.wes.ServiceInfo;
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

    DrsConfig drsConfig = new DrsConfig();

    String fileMappingDirectory = System.getProperty("java.io.tmpdir") + "/cromwell-object-mappings";

}
