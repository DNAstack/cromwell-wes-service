package com.dnastack.wes.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@EqualsAndHashCode
@NoArgsConstructor
@ToString
@Builder
public class RunRequest {

    @JsonProperty("workflow_params")
    Map<String, Object> workflowParams;

    @JsonProperty("workflow")
    String workflowType;


    @JsonProperty("workflow_type_version")
    String workflowTypeVersion;

    @JsonProperty("tags")
    Map<String, String> tags;

    @JsonProperty("workflow_engine_parameters")
    Map<String, String> workflowEngineParameters;

    @JsonProperty("workflow_url")
    String workflowUrl;

    @JsonIgnore
    MultipartFile[] workflowAttachments;

}
