package com.dnastack.wes.agent;

import com.dnastack.wes.api.RunLog;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@JsonTypeName("UPDATE_WORKFLOW_STATUS")
@AllArgsConstructor
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class UpdateWorkflowStatusAgentReport extends AgentReport {

    @JsonUnwrapped
    private RunLog runLog;


    @Override
    public AgentReportType getType() {
        return AgentReportType.UPDATE_WORKFLOW_STATUS;
    }

}
