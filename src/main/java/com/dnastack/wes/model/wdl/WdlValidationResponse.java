package com.dnastack.wes.model.wdl;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Getter
@Setter
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
public class WdlValidationResponse {

    @JsonProperty("workflow_name")
    private String workflowName;

    @JsonProperty("meta")
    private Map<String, Object> meta;

    @JsonProperty("parameter_meta")
    private Map<String, Object> parameterMeta;

    @JsonProperty("errors")
    private List<WdlValidationError> errors;

    @JsonProperty("workflow_inputs")
    private List<WdlField> workflowInputs;

    @JsonProperty("workflow_outputs")
    private List<WdlField> workflowOutputs;

    @JsonProperty("valid")
    private Boolean valid;

}
