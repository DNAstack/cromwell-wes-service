package com.dnastack.wes.agent;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Set;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder()
@JsonTypeName("HEALTH_CHECK")
@AllArgsConstructor
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class HealthCheckAgentReport extends AgentReport {

    private Set<Check> checks;

    @Override
    public AgentReportType getType() {
        return AgentReportType.HEALTH_CHECK;
    }

}
