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

    public static RunLog mapMetadataToRunLog(CromwellMetadataResponse metadataResponse, Map<String, Object> mappedFileObject) {
        RunLog runLog = new RunLog();
        runLog.setRunId(metadataResponse.getId());
        runLog.setState(mapState(metadataResponse.getStatus()));
        runLog.setOutputs(metadataResponse.getOutputs());

        String workflowStart = metadataResponse.getStart() == null ? null : metadataResponse.getStart().toString();
        String workflowEnd = metadataResponse.getEnd() == null ? null : metadataResponse.getEnd().toString();
        Log workflowLog = Log.builder().startTime(workflowStart).endTime(workflowEnd)
            .name(metadataResponse.getWorkflowName()).build();
        runLog.setRunLog(workflowLog);
        runLog.setTaskLogs(mapTaskCallsToLog(metadataResponse.getCalls()));
        runLog.setRequest(mapMetadataToRunRequest(metadataResponse, mappedFileObject));

        return runLog;
    }

    private static RunRequest mapMetadataToRunRequest(CromwellMetadataResponse metadataResponse, Map<String, Object> mappedFileObject) {
        RunRequest runRequest = new RunRequest();
        runRequest.setWorkflowType(metadataResponse.getActualWorkflowLanguage());
        runRequest.setWorkflowTypeVersion(metadataResponse.getActualWorkflowLanguageVersions());
        Map<String, Object> options = getWorkflowOptions(metadataResponse);
        runRequest.setWorkflowParams(mapCromwellInputsToOriginalValues(metadataResponse
            .getSubmittedFiles(), mappedFileObject));
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

    private static Map<String, Object> mapCromwellInputsToOriginalValues(Map<String, String> submittedFiles,
        Map<String, Object> mappedFileObject) {
        Map<String, Object> inputs = Collections.emptyMap();

        try {
            String rawInputs = submittedFiles.get("inputs");
            if (rawInputs != null) {
                inputs = objectMapper.readValue(rawInputs, new TypeReference<Map<String, Object>>() {
                });
                if (mappedFileObject != null) {
                    return inputs.entrySet().stream()
                        .map(entry -> {
                            entry.setValue(mapFileObject(mappedFileObject, entry.getValue()));
                            return entry;
                        }).collect(HashMap::new, (m, v) -> m.put(v.getKey(), v.getValue()), HashMap::putAll);
                }
            }
        } catch (Exception e) {

        }
        return inputs;

    }


    private static Object mapFileObject(Map<String, Object> originalFiles, Object objectToMap) {
        if (objectToMap == null) {
            return null;
        }
        if (objectToMap instanceof String && originalFiles.containsKey(objectToMap)) {
            return originalFiles.get(objectToMap);
        } else if (objectToMap instanceof List) {
            List listObjectsToMap = (List) objectToMap;
            List<Object> returnObjects = new ArrayList<>();
            for (Object object : listObjectsToMap) {
                returnObjects.add(mapFileObject(originalFiles, object));
            }
            return returnObjects;
        } else if (objectToMap instanceof Map) {
            Map<String, Object> mapObjectsToMap = (Map<String, Object>) objectToMap;
            Map<String, Object> returnObjects = new HashMap<>();
            for (Entry<String, Object> entry : mapObjectsToMap.entrySet()) {
                returnObjects.put(entry.getKey(), mapFileObject(originalFiles, entry.getValue()));
            }
            return returnObjects;
        }
        return objectToMap;
    }


    public static List<Log> mapTaskCallsToLog(Map<String, List<CromwellTaskCall>> calls) {
        List<Log> taskLogs = new ArrayList<>();

        if (calls != null) {
            for (Entry<String, List<CromwellTaskCall>> entry : calls.entrySet()) {
                for (CromwellTaskCall taskCall : entry.getValue()) {
                    taskLogs.add(mapTaskCallToLog(entry.getKey(), taskCall));
                }
            }
        }
        return taskLogs;
    }

    public static Log mapTaskCallToLog(String name, CromwellTaskCall taskCall) {

        String taskStart = taskCall.getStart() == null ? null : taskCall.getStart().toString();
        String taskEnd = taskCall.getEnd() == null ? null : taskCall.getEnd().toString();
        return Log.builder().name(name).exitCode(taskCall.getReturnCode()).cmd(taskCall.getCommandLine())
            .startTime(taskStart).endTime(taskEnd).stderr(taskCall.getStderr()).stdout(taskCall.getStdout()).build();

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
