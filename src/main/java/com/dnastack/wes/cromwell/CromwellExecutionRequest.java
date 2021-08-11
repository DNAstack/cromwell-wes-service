package com.dnastack.wes.cromwell;

import feign.form.FormProperty;
import java.io.File;
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
@ToString
@EqualsAndHashCode
@Builder
public class CromwellExecutionRequest {


    @FormProperty("workflowUrl")
    String workflowUrl;

    @FormProperty("workflowOnHold")
    Boolean workflowOnHold;

    @FormProperty("workflowSource")
    String workflowSource;

    @FormProperty("workflowDependencies")
    File workflowDependencies;

    @FormProperty("workflowOptions")
    Map<String, Object> workflowOptions;

    @FormProperty("workflowInputs")
    Map<String, Object> workflowInputs;

    @FormProperty("labels")
    Map<String, String> labels;

}
