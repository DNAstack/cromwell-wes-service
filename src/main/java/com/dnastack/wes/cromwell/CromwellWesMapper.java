package com.dnastack.wes.cromwell;

import com.dnastack.wes.api.*;
import com.dnastack.wes.security.XForwardUtil;
import com.dnastack.wes.translation.PathTranslator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.Map.Entry;

@Slf4j
@Service
public class CromwellWesMapper {

    private static final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final CromwellConfig cromwellConfig;

    public CromwellWesMapper(CromwellConfig cromwellConfig) {
        this.cromwellConfig = cromwellConfig;
    }

    public static RunListResponse mapCromwellResponseToRunListResposne(CromwellResponse response) {
        return RunListResponse.builder()
            .runs(response.getResults().stream().map(CromwellWesMapper::mapCromwellStatusToRunStatus)
                .toList()).build();
    }

    public static RunStatus mapCromwellStatusToRunStatus(CromwellStatus status) {
        return RunStatus.builder().runId(status.getId()).state(mapState(status.getStatus())).build();
    }

    public static State mapState(String cromwellState) {
        cromwellState = cromwellState.toLowerCase();
        return switch (cromwellState) {
            case "on hold" -> State.QUEUED;
            case "succeeded" -> State.COMPLETE;
            case "submitted" -> State.INITIALIZING;
            case "running" -> State.RUNNING;
            case "aborting" -> State.CANCELING;
            case "aborted" -> State.CANCELED;
            case "failed" -> State.EXECUTOR_ERROR;
            default -> State.UNKNOWN;
        };

    }

    public RunLog mapMetadataToRunLog(
        CromwellMetadataResponse metadataResponse,
        Map<String, Object> originalInputs,
        List<PathTranslator> pathTranslators
    ) {
        RunLog runLog = new RunLog();
        runLog.setRunId(metadataResponse.getId());
        runLog.setState(mapState(metadataResponse.getStatus()));

        runLog.setOutputs(translatePaths(new TypeReference<Map<String, Object>>() {
        }, metadataResponse.getOutputs(), pathTranslators));

        Log workflowLog = Log.builder().startTime(metadataResponse.getStart()).endTime(metadataResponse.getEnd())
            .name(metadataResponse.getWorkflowName()).build();
        if (metadataResponse.getFailures() != null) {
            workflowLog.setStderr(ServletUriComponentsBuilder.fromCurrentRequest().query(null).pathSegment("logs", "stderr").build().toString());
        }
        runLog.setRunLog(workflowLog);
        runLog.setTaskLogs(mapTaskCallsToLog(metadataResponse.getCalls()));
        runLog.setRequest(mapMetadataToRunRequest(metadataResponse, originalInputs));

        return runLog;
    }

    private <T> T translatePaths(TypeReference<T> typeReference, T objectToTranslate, List<PathTranslator> translators) {
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

    public List<Log> mapTaskCallsToLog(Map<String, List<CromwellTaskCall>> calls) {
        List<Log> taskLogs = new ArrayList<>();

        if (calls != null) {
            for (Entry<String, List<CromwellTaskCall>> entry : calls.entrySet()) {
                List<CromwellTaskCall> taskCalls = entry.getValue();

                for (int i = 0; i < taskCalls.size(); i++) {
                    CromwellTaskCall taskCall = taskCalls.get(i);
                    String uniqueName = taskCalls.size() > 1 ? entry.getKey() + "-" + i : entry.getKey();
                    taskLogs.add(mapTaskCallToLog(entry.getKey(), i, uniqueName, taskCall));
                }
            }
        }
        return taskLogs;
    }

    private RunRequest mapMetadataToRunRequest(CromwellMetadataResponse metadataResponse, Map<String, Object> originalInputs) {
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
            .containsKey(cromwellConfig.getWorkflowUrlLabel())) {
            runRequest.setWorkflowUrl(metadataResponse.getLabels().get(cromwellConfig.getWorkflowUrlLabel()));
        }
        metadataResponse.getLabels().remove(cromwellConfig.getWorkflowUrlLabel());

        runRequest.setTags(metadataResponse.getLabels());

        return runRequest;
    }

    public Log mapTaskCallToLog(String name, Integer index, String uniqueName, CromwellTaskCall taskCall) {
        HttpServletRequest request =
            ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes()))
                .getRequest();
        String path = request.getRequestURI();
        String stdout = XForwardUtil.getExternalPath(request, Paths.get(path, "logs/task/" + name + "/" + index.toString() + "/stdout").toString());
        String stderr = XForwardUtil.getExternalPath(request, Paths.get(path, "logs/task/" + name + "/" + index.toString() + "/stderr").toString());

        return Log.builder().name(uniqueName).exitCode(taskCall.getReturnCode()).cmd(taskCall.getCommandLine())
            .startTime(taskCall.getStart()).endTime(taskCall.getEnd()).stderr(stderr).stdout(stdout).build();
    }

    private Map<String, Object> getWorkflowOptions(CromwellMetadataResponse metadataResponse) {
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

    private Map<String, String> mapOptionsToEngineParameters(Map<String, Object> workflowOptions) {
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


}
