package com.dnastack.wes.agent;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = SubmitWorkflowAgentWork.class, name = "SUBMIT_WORKFLOW"),
    @JsonSubTypes.Type(value = UpdateWorkflowStatusAgentWork.class, name = "UPDATE_WORKFLOW_STATUS"),
    @JsonSubTypes.Type(value = CancelWorkflowAgentWork.class, name = "CANCEL_WORKFLOW"),
    @JsonSubTypes.Type(value = HealthCheckAgentWork.class, name = "HEALTH_CHECK"),
})
@SuperBuilder
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public abstract class AgentWork {

    private AgentWorkType type;
    private String workId;
    private Instant createdAt;

}
