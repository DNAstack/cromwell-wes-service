package com.dnastack.wes.drs;

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
@ConfigurationProperties("app.drs")
public class DrsConfig {

    List<String> supportedTypes = Arrays.asList(AccessType.file.name());


}
