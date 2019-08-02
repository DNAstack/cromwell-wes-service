package com.dnastack.wes.config;

import com.dnastack.wes.model.drs.AccessType;
import java.util.Arrays;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties("wes.drs")
public class DrsConfig {

    /**
     * List of supported DRS access types which this WES service can understand. Any DRS object provided which does
     * not have one of these accessTypes will either be ignored or result in an error. A DRS Access type corresponds
     * to an object Source
     * @see AccessType
     */
    List<String> supportedAccessTypes = Arrays.asList(AccessType.file.name());


}
