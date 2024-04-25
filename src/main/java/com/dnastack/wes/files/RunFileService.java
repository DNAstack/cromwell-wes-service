package com.dnastack.wes.files;

import com.dnastack.wes.api.ErrorResponse;
import com.dnastack.wes.cromwell.CromwellMetadataResponse;
import com.dnastack.wes.cromwell.CromwellService;
import com.dnastack.wes.cromwell.CromwellTaskCall;
import com.dnastack.wes.shared.NotFoundException;
import com.dnastack.wes.storage.BlobMetadata;
import com.dnastack.wes.storage.BlobStorageClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.HttpRange;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

@Slf4j
@Service
public class RunFileService {


    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();
    final BlobStorageClient storageClient;
    final CromwellService cromwellService;
    private final TaskExecutor defaultAsyncOperationExecutor;

    final Configuration jsonPathConfiguration = Configuration.builder()
        .jsonProvider(new JacksonJsonNodeJsonProvider())
        .options(Option.SUPPRESS_EXCEPTIONS)
        .build();

    public RunFileService(BlobStorageClient storageClient, CromwellService cromwellService, TaskExecutor defaultAsyncOperationExecutor) {
        this.storageClient = storageClient;
        this.cromwellService = cromwellService;
        this.defaultAsyncOperationExecutor = defaultAsyncOperationExecutor;
    }


    /**
     * Get the files for a specific run.
     *
     * @param runId The cromwell id
     *
     * @return a list of generated files for the run
     */
    public RunFiles getRunFiles(String runId) {
        CromwellMetadataResponse metadataResponse = cromwellService.getMetadata(runId);
        Set<String> finalFileSet = new HashSet<>();
        Set<String> secondaryFileSet = new HashSet<>();
        Set<String> logFileSet = new HashSet<>();
        Set<String> inputFileSet = new HashSet<>();
        List<RunFile> files = new ArrayList<>();

        Map<String, Object> outputs = metadataResponse.getOutputs();
        Map<String, Object> inputs = metadataResponse.getInputs();
        if (outputs != null && !outputs.isEmpty()) {
            outputs.values().forEach(output -> extractFilesFromValue(finalFileSet, OBJECT_MAPPER.valueToTree(output)));
        }

        if (inputs != null && !inputs.isEmpty()) {
            inputs.values().forEach(input -> extractFilesFromValue(inputFileSet, OBJECT_MAPPER.valueToTree(input)));
        }

        extractSecondaryLogFiles(secondaryFileSet, logFileSet, metadataResponse);

        finalFileSet.forEach(path -> files.add(new RunFile(RunFile.FileType.FINAL, path)));

        inputFileSet.stream().filter(path -> !finalFileSet.contains(path)).map(path -> new RunFile(RunFile.FileType.INPUT, path)).forEach(files::add);
        logFileSet.stream().filter(path -> !finalFileSet.contains(path)).map(path -> new RunFile(RunFile.FileType.LOG, path)).forEach(files::add);
        secondaryFileSet.stream()
            .filter(path -> !logFileSet.contains(path) && !inputFileSet.contains(path) && !finalFileSet.contains(path))
            .map(path -> new RunFile(RunFile.FileType.SECONDARY, path)).forEach(files::add);

        return new RunFiles(files);
    }

    /**
     * Request to delete the files associated with the run.
     *
     * @param runId The cromwell id
     *
     * @return the cromwell id
     */
    public RunFileDeletions deleteRunFiles(String runId, boolean async) {
        List<RunFile> files = getRunFiles(runId).runFiles();
        List<RunFileDeletion> outcomes = files.stream().filter(runFile -> RunFile.FileType.SECONDARY.equals(runFile.getFileType()))
            .map(runFile -> {
                if (async) {
                    return deleteRunFileAsync(runFile);
                } else {
                    return deleteRunFile(runFile);
                }
            }).toList();
        return new RunFileDeletions(outcomes);
    }

    public RunFileDeletion deleteRunFileAsync(RunFile runFile) {
        CompletableFuture.runAsync(() -> deleteRunFile(runFile), defaultAsyncOperationExecutor);
        return new RunFileDeletion(runFile, RunFileDeletion.DeletionState.ASYNC, null);
    }

    public RunFileDeletion deleteRunFile(RunFile runFile) {
        try {
            storageClient.deleteFile(runFile.getPath());
            log.debug("Deleting file '{}'", runFile.getPath());
            return new RunFileDeletion(runFile, RunFileDeletion.DeletionState.DELETED, null);
        } catch (NotFoundException e){
            log.debug("File '{}' was not found, skipping deletion", runFile.getPath());
            return new RunFileDeletion(runFile, RunFileDeletion.DeletionState.NOT_FOUND, ErrorResponse.builder().errorCode(404).msg(e.getMessage()).build());
        } catch (Exception e) {
            log.debug("Encountered exception while deleting file '%s': '%s'".formatted(runFile.getPath(), e.getMessage()), e);
            return new RunFileDeletion(runFile, RunFileDeletion.DeletionState.FAILED, ErrorResponse.builder().errorCode(400).msg(e.getMessage()).build());
        }
    }

    public RunFile getRunFile(String runId, String path) {
        if (path.startsWith("$")) {
            path = evaluateJsonPath(runId, path);
        }

        final String finalPath = path;
        RunFile file = getRunFiles(runId).runFiles()
            .stream()
            .filter(runFile -> Objects.equals(runFile.getPath(), finalPath))
            .findFirst()
            .orElseThrow(() -> new NotFoundException("File %s was not an input or output of the run %s and is not accessible".formatted(finalPath, runId)));

        BlobMetadata metadata = storageClient.getBlobMetadata(file.getPath());
        file.setBlobMetadata(metadata);
        return file;
    }

    private String evaluateJsonPath(String runId, String jsonPath) {
        String metadataResponse;
        try {
            metadataResponse = OBJECT_MAPPER.writeValueAsString(cromwellService.getRun(runId));
        } catch (JsonProcessingException e) {
            throw new NotFoundException("Could not find value corresponding to json path expression %s".formatted(jsonPath));
        }
        JsonNode foundNode = JsonPath.using(jsonPathConfiguration).parse(metadataResponse).read(jsonPath);
        if (foundNode != null && foundNode.isTextual()) {
            return foundNode.asText();
        } else {
            throw new NotFoundException("Could not find value corresponding to json path expression %s".formatted(jsonPath));
        }
    }

    public void streamRunFile(HttpServletResponse response, String runId, String path, HttpRange httpRange) throws IOException {
        RunFile runFile = getRunFile(runId, path);
        long contentLength = runFile.getBlobMetadata().getSize();
        if (httpRange != null) {
            long start = httpRange.getRangeStart(runFile.getBlobMetadata().getSize());
            long end = httpRange.getRangeEnd(runFile.getBlobMetadata().getSize());
            contentLength = end - start + 1;
            if (contentLength != runFile.getBlobMetadata().getSize()) {
                response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
                response.setHeader("Content-Range", "bytes " + start + "-" + end + "/" + contentLength);
            }
        }
        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        response.setHeader("Content-Length", String.valueOf(contentLength));
        if (runFile.getBlobMetadata().getLastModifiedTime() != null) {
            response.setHeader("Last-Modified", String.valueOf(runFile.getBlobMetadata().getLastModifiedTime().toEpochMilli()));
        }
        storageClient.readBytes(response.getOutputStream(), runFile.getPath(), httpRange);
    }

    private void extractSecondaryLogFiles(Set<String> secondaryFileSet, Set<String> logFileSet, CromwellMetadataResponse metadataResponse) {
        Map<String, Object> outputs = metadataResponse.getOutputs();
        if (outputs != null && !outputs.isEmpty()) {
            outputs.values().forEach(output -> extractFilesFromValue(secondaryFileSet, OBJECT_MAPPER.valueToTree(output)));
        }
        Map<String, List<CromwellTaskCall>> calls = metadataResponse.getCalls();
        if (calls != null && !calls.isEmpty()) {
            calls.values().stream().flatMap(List::stream).forEach(call -> extractSecondaryLogFilesFromCall(secondaryFileSet, logFileSet, call));
        }
    }

    private void extractSecondaryLogFilesFromCall(Set<String> secondaryFileSet, Set<String> logFileSet, CromwellTaskCall call) {
        Map<String, Object> outputs = call.getOutputs();
        if (outputs != null && !outputs.isEmpty()) {
            outputs.values().forEach(output -> extractFilesFromValue(secondaryFileSet, OBJECT_MAPPER.valueToTree(output)));
        }

        Stream.of(call.getStderr(), call.getStdout())
            .filter(Objects::nonNull)
            .filter(storageClient::isFile)
            .forEach(logFileSet::add);

        Map<String, String> backendLogs = call.getBackendLogs();
        if (backendLogs != null && !backendLogs.isEmpty()) {
            backendLogs.values().forEach(log -> extractFilesFromValue(logFileSet, OBJECT_MAPPER.valueToTree(log)));
        }
        CromwellMetadataResponse subWorkflowMetadata = call.getSubWorkflowMetadata();
        if (subWorkflowMetadata != null) {
            extractSecondaryLogFiles(secondaryFileSet, logFileSet, subWorkflowMetadata);
        }
    }

    private void extractFilesFromValue(Set<String> fileSet, JsonNode node) {
        if (node.isTextual()) {
            if (storageClient.isFile(node.asText())) {
                fileSet.add(node.asText());
            }
        } else if (node.isArray()) {
            extractFilesFromArrayNode(fileSet, (ArrayNode) node);
        } else if (node.isObject()) {
            extractFilesFromObjectNode(fileSet, (ObjectNode) node);
        }
    }

    private void extractFilesFromArrayNode(Set<String> fileSet, ArrayNode outputs) {
        outputs.forEach(output -> extractFilesFromValue(fileSet, output));
    }

    private void extractFilesFromObjectNode(Set<String> fileSet, ObjectNode outputs) {
        outputs.forEach(output -> extractFilesFromValue(fileSet, output));
    }

}
