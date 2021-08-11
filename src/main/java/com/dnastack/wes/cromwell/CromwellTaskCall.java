package com.dnastack.wes.cromwell;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Patrick Magee created on: 2018-09-24
 */
@Getter
@Setter
public class CromwellTaskCall {

    private String executionStatus;
    private String backendStatus;
    private String commandLine;
    private Integer attempt;
    private Boolean preemptible;
    private Integer shardIndex;
    private ZonedDateTime start;
    private ZonedDateTime end;
    private String jobId;
    private String backend;
    private String callRoot;
    private Integer returnCode;


    private List<CromwellFailure> failures;

    private String stderr;
    private String stdout;
    private Map<String, String> jes;
    private Map<String, String> runtimeAttributes;
    private Map<String, Object> inputs;
    private Map<String, Object> outputs;
    private Map<String, String> backendLogs;
    private Map<String, String> backendLabels;
    private Map<String, String> labels;
}
