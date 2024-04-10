package com.dnastack.wes.agent;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Data
@NoArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = UpdateWorkflowStatusAgentReport.class, name = "UPDATE_WORKFLOW_STATUS"),
    @JsonSubTypes.Type(value = HealthCheckAgentReport.class, name = "HEALTH_CHECK"),
})
@SuperBuilder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public abstract class AgentReport {

    private AgentReportType type;
    private String reportId;
    // Set if his is a response to a specific work order
    private String workId;
    @Builder.Default
    private Instant createdAt = Instant.now();

}
