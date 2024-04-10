package com.dnastack.wes.agent;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;

import java.util.List;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SubmitWorkflowAgentWork extends AgentWork {

    private String workflowUrl;
    private String workflowType;
    private String workflowTypeVersion;
    private Map<String,Object> workflowParams;
    private Map<String,String> workflowEngineParameters;
    private List<Base64EncodedAttachments> workflowFiles;

    @Override
    public AgentWorkType getType() {
        return AgentWorkType.SUBMIT_WORKFLOW;
    }

}
