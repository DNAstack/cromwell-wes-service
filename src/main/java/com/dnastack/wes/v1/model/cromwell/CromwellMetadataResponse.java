package com.dnastack.wes.v1.model.cromwell;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * @author Patrick Magee
 * created on: 2018-09-24
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class CromwellMetadataResponse {

    private String id;
    private ZonedDateTime submission;
    private String actualWorkflowLanguageVersions;
    private String actualWorkflowLanguage;
    private String status;
    private String workflowName;
    private String workflowRoot;
    private ZonedDateTime start;
    private ZonedDateTime end;

    private Map<String,String> submittedFiles;
    private Map<String, List<CromwellTaskCall>> calls;
    private Map<String,String> labels;
    private Map<String, Object> outputs;
    private Map<String,Object> inputs;
    private List<CromwellFailure> failures;
}
