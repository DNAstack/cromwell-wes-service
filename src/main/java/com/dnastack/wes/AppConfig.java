package com.dnastack.wes;

import com.dnastack.wes.model.wes.ServiceInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties("app")
public class AppConfig {

    private static final ObjectMapper mapper = new ObjectMapper();

    ServiceInfo serviceInfo = new ServiceInfo();

    Boolean restrictUserAccessToOwnRuns = false;

    public ServiceInfo getServiceInfo() {
        try {
            return mapper.readValue(mapper.writeValueAsString(serviceInfo), ServiceInfo.class);
        } catch (Exception e) {
            return serviceInfo;
        }
    }
}
