package com.dnastack.wes.v1.model.wes;


import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Builder
public class ServiceInfo {

    @JsonProperty("workflow_type_versions")
    Map<String, List<String>> workflowTypeVersions;

    @JsonProperty("supported_wes_versions")
    List<String> supportedWesVersions;

    @JsonProperty("supported_filesystem_protocols")
    List<String> supportedFileSystemProtocols;

    @JsonProperty("workflow_engine_versions")
    Map<String, String> workflowEngineVersions;

    @JsonProperty("default_workflow_engine_parameters")
    List<DefaultWorkflowEngineParameter> defaultWorkflowEngineParameters;

    @JsonProperty("system_state_counts")
    Map<State, Integer> systemStateCounts;

    @JsonProperty("auth_instruction_url")
    String authInstructionUrl;

    @JsonProperty("contact_info_url")
    String contactInforUrl;

    @JsonProperty("tags")
    Map<String, String> tags;

}
