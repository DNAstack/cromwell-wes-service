package com.dnastack.wes.cromwell;

import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

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
    private Instant submission;
    private String actualWorkflowLanguageVersions;
    private String actualWorkflowLanguage;
    private String status;
    private String workflowName;
    private String workflowRoot;
    private Instant start;
    private Instant end;
    private String parentWorkflowId;
    private Map<String, String> submittedFiles;
    private Map<String, List<CromwellTaskCall>> calls;
    private Map<String, String> labels;
    private Map<String, Object> outputs;
    private Map<String, Object> inputs;
    private List<CromwellFailure> failures;

}
