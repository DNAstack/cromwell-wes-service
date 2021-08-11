package com.dnastack.wes.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.web.multipart.MultipartFile;

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
