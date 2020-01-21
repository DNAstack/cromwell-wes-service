package com.dnastack.wes.service;

import com.dnastack.wes.Constants;
import com.dnastack.wes.model.cromwell.CromwellMetadataResponse;
import com.dnastack.wes.model.cromwell.CromwellResponse;
import com.dnastack.wes.model.cromwell.CromwellStatus;
import com.dnastack.wes.model.cromwell.CromwellTaskCall;
import com.dnastack.wes.model.wes.Log;
import com.dnastack.wes.model.wes.RunListResponse;
import com.dnastack.wes.model.wes.RunLog;
import com.dnastack.wes.model.wes.RunRequest;
import com.dnastack.wes.model.wes.RunStatus;
import com.dnastack.wes.model.wes.State;
import com.dnastack.wes.wdl.PathTranslator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CromwellWesMapper {

    private final static ObjectMapper objectMapper = new ObjectMapper();

    public static RunListResponse mapCromwellResponseToRunListResposne(CromwellResponse response) {
        return RunListResponse.builder()
            .runs(response.getResults().stream().map(CromwellWesMapper::mapCromwellStatusToRunStatus)
                .collect(Collectors.toList())).build();
    }

    public static RunStatus mapCromwellStatusToRunStatus(CromwellStatus status) {
        return RunStatus.builder().runId(status.getId()).state(mapState(status.getStatus())).build();
    }


    public static RunLog mapMetadataToRunLog(CromwellMetadataResponse metadataResponse, Map<String, Object> originalInputs) {
        return mapMetadataToRunLog(metadataResponse, originalInputs, null);
    }

    public static RunLog mapMetadataToRunLog(CromwellMetadataResponse metadataResponse, Map<String, Object> originalInputs, List<PathTranslator> pathTranslators) {
        RunLog runLog = new RunLog();
        runLog.setRunId(metadataResponse.getId());
        runLog.setState(mapState(metadataResponse.getStatus()));

        runLog.setOutputs(translatePaths(new TypeReference<Map<String, Object>>() {
        }, metadataResponse.getOutputs(), pathTranslators));

        String workflowStart = metadataResponse.getStart() == null ? null : metadataResponse.getStart().toString();
        String workflowEnd = metadataResponse.getEnd() == null ? null : metadataResponse.getEnd().toString();
        Log workflowLog = Log.builder().startTime(workflowStart).endTime(workflowEnd)
            .name(metadataResponse.getWorkflowName()).build();
        runLog.setRunLog(workflowLog);
        runLog.setTaskLogs(mapTaskCallsToLog(metadataResponse.getCalls(), pathTranslators));
        runLog.setRequest(mapMetadataToRunRequest(metadataResponse, originalInputs));

        return runLog;
    }

    private static <T> T translatePaths(TypeReference<T> typeReference, T objectToTranslate, List<PathTranslator> translators) {
        if (translators != null && !translators.isEmpty()) {
            JsonNode node = objectMapper.valueToTree(objectToTranslate);

            for (PathTranslator translator : translators) {
                if (translator.shouldMapJsonNode(node)) {
                    node = translator.mapJsonNode(node);
                }
            }
            return objectMapper.convertValue(node, typeReference);
        }
        return objectToTranslate;
    }

    private static RunRequest mapMetadataToRunRequest(CromwellMetadataResponse metadataResponse, Map<String, Object> originalInputs) {
        RunRequest runRequest = new RunRequest();
        runRequest.setWorkflowType(metadataResponse.getActualWorkflowLanguage());
        runRequest.setWorkflowTypeVersion(metadataResponse.getActualWorkflowLanguageVersions());
        Map<String, Object> options = getWorkflowOptions(metadataResponse);

        Map<String, Object> params = metadataResponse.getInputs();
        if (originalInputs != null) {
            params = originalInputs;
        }

        if (params == null) {
            params = Collections.emptyMap();
        }

        runRequest.setWorkflowParams(params);
        runRequest.setWorkflowEngineParameters(mapOptionsToEngineParameters(options));

        if (metadataResponse.getLabels() != null && metadataResponse.getLabels()
            .containsKey(Constants.WORKFLOW_URL_LABEL)) {
            runRequest.setWorkflowUrl(metadataResponse.getLabels().get(Constants.WORKFLOW_URL_LABEL));
            metadataResponse.getLabels().remove(Constants.WORKFLOW_URL_LABEL);
        }

        runRequest.setTags(metadataResponse.getLabels());

        return runRequest;
    }


    private static Map<String, Object> getWorkflowOptions(CromwellMetadataResponse metadataResponse) {
        Map<String, String> submittedFiles = metadataResponse.getSubmittedFiles();
        Map<String, Object> optionsMap = new HashMap<>();
        if (metadataResponse.getWorkflowRoot() != null && !metadataResponse.getWorkflowRoot().isEmpty()) {
            optionsMap.put("workflow_root", metadataResponse.getWorkflowRoot());
        }

        if (submittedFiles != null) {
            String options = submittedFiles.getOrDefault("options", "{}");
            try {
                optionsMap.putAll(objectMapper.readValue(options, new TypeReference<Map<String, Object>>() {
                }));
            } catch (IOException e) {
                log.error(e.getMessage(), e);

            }
        }
        return optionsMap;
    }

    private static Map<String, String> mapOptionsToEngineParameters(Map<String, Object> workflowOptions) {
        Map<String, String> engineParams = new HashMap<>();
        for (Entry<String, Object> entry : workflowOptions.entrySet()) {
            try {
                JsonNode node = objectMapper.valueToTree(entry.getValue());
                if (node.isObject() || node.isArray() || node.isPojo()) {
                    engineParams.put(entry.getKey(), objectMapper.writeValueAsString(entry.getValue()));
                } else {
                    engineParams.put(entry.getKey(), entry.getValue().toString());
                }
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }

        }
        return engineParams;

    }


    public static List<Log> mapTaskCallsToLog(Map<String, List<CromwellTaskCall>> calls, List<PathTranslator> pathTranslators) {
        List<Log> taskLogs = new ArrayList<>();

        if (calls != null) {
            for (Entry<String, List<CromwellTaskCall>> entry : calls.entrySet()) {
                for (CromwellTaskCall taskCall : entry.getValue()) {
                    taskLogs.add(mapTaskCallToLog(entry.getKey(), taskCall, pathTranslators));
                }
            }
        }
        return taskLogs;
    }

    public static Log mapTaskCallToLog(String name, CromwellTaskCall taskCall, List<PathTranslator> pathTranslators) {

        String taskStart = taskCall.getStart() == null ? null : taskCall.getStart().toString();
        String taskEnd = taskCall.getEnd() == null ? null : taskCall.getEnd().toString();
        String stdout = translatePaths(new TypeReference<String>() {
        }, taskCall.getStdout(), pathTranslators);
        String stderr = translatePaths(new TypeReference<String>() {
        }, taskCall.getStderr(), pathTranslators);
        return Log.builder().name(name).exitCode(taskCall.getReturnCode()).cmd(taskCall.getCommandLine())
            .startTime(taskStart).endTime(taskEnd).stderr(stderr).stdout(stdout).build();

    }

    public static State mapState(String cromwellState) {
        cromwellState = cromwellState.toLowerCase();
        switch (cromwellState) {
            case "on hold":
                return State.QUEUED;
            case "succeeded":
                return State.COMPLETE;
            case "submitted":
                return State.INITIALIZING;
            case "running":
                return State.RUNNING;
            case "aborting":
                return State.CANCELINGSTATE;
            case "aborted":
                return State.CANCELED;
            case "failed":
                return State.EXECUTOR_ERROR;
            default:
                return State.UNKNOWN;

        }
    }


}
