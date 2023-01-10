package com.dnastack.wes;

import com.dnastack.wes.cromwell.CromwellConfig;
import com.dnastack.wes.translation.PathTranslationConfig;
import com.dnastack.wes.api.ServiceInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Configuration
@ConfigurationProperties("wes")
public class AppConfig {

    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * The ServiceInfo object provides the static content to display to the user when they visit the {@code ga4gh/wes
     * /v1/serviceInfo} endpoint
     */
    ServiceInfo serviceInfo = new ServiceInfo();


    Integer defaultPageSize = 20;
    Integer defaultPage = 1;

    /**
     *
     */
    List<PathTranslationConfig> pathTranslations = new ArrayList<>();


    public ServiceInfo getServiceInfo() {
        try {
            return mapper.readValue(mapper.writeValueAsString(serviceInfo), ServiceInfo.class);
        } catch (Exception e) {
            return serviceInfo;
        }
    }

}
