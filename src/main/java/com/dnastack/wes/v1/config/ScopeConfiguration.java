package com.dnastack.wes.v1.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Configuration
public class ScopeConfiguration {

    private String runsExecute = "SCOPE_runs:execute";

    private String runsGetAny = "SCOPE_runs:get:any";

    private String runsGetCreated = "SCOPE_runs:get:created";

    private String runsCancelAny = "SCOPE_runs:cancel:any";

    private String runsCancelCreated = "SCOPE_runs:cancel:created";


}
