package com.dnastack.wes.config;

import com.dnastack.wes.model.wes.ServiceInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.context.annotation.Configuration;

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

    /**
     * The API can be used in a very rudimentary "multi-tenant" mode. When enabled, the api will restrict results to a
     * client to those which correspond to the correct principal. For example, when listing runs, only runs submitted by
     * the client will be shown when this property is set to true. Conversely, when set to false, ALL runs irrespective
     * of who the client is will be returned.
     */
    Boolean enableMultiTenantSupport = false;


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
