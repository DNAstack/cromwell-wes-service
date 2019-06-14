package com.dnastack.wes.service;

import com.dnastack.wes.client.CromwellClient;
import com.dnastack.wes.client.WdlValidatorClient;
import com.dnastack.wes.exception.InvalidRequestException;
import com.dnastack.wes.mapper.CromwellWesMapper;
import com.dnastack.wes.model.cromwell.CromwellExecutionRequest;
import com.dnastack.wes.model.cromwell.CromwellMetadataResponse;
import com.dnastack.wes.model.cromwell.CromwellResponse;
import com.dnastack.wes.model.cromwell.CromwellSearch;
import com.dnastack.wes.model.cromwell.CromwellStatus;
import com.dnastack.wes.model.cromwell.CromwellVersion;
import com.dnastack.wes.model.wdl.WdlValidationRequest;
import com.dnastack.wes.model.wdl.WdlValidationResponse;
import com.dnastack.wes.model.wdl.WdlWorkflowDependency;
import com.dnastack.wes.model.wes.RunId;
import com.dnastack.wes.model.wes.RunListResponse;
import com.dnastack.wes.model.wes.RunLog;
import com.dnastack.wes.model.wes.RunRequest;
import com.dnastack.wes.model.wes.RunStatus;
import com.dnastack.wes.model.wes.State;
import com.dnastack.wes.utils.Constants;
import com.dnastack.wes.wdl.FileMapper;
import com.dnastack.wes.wdl.FileWrapper;
import com.dnastack.wes.wdl.WdlFileProcessor;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import feign.FeignException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
public class CromwellService {

    private final CromwellClient client;
    private final WdlValidatorClient validatorClient;
    private final ObjectMapper mapper;

    @Autowired
    CromwellService(CromwellClient cromwellClient, WdlValidatorClient validatorClient, ObjectMapper mapper) {
        this.client = cromwellClient;
        this.validatorClient = validatorClient;
        this.mapper = mapper;
    }


    /**
     * List workflows from the associated cromwell service and aggregate them into system counts
     */
    public Map<State, Integer> getSystemStateCounts() {
        CromwellResponse response = client.listWorkflows();
        Map<State, Integer> systemCounts = new HashMap<>();
        response.getResults().stream().forEach(cromwellStatus -> {
            State state = CromwellWesMapper.mapState(cromwellStatus.getStatus());
            systemCounts.compute(state, (key, count) -> {
                if (count != null) {
                    return count + 1;
                }
                return 1;
            });
        });

        return systemCounts;
    }


    /**
     * Retrieve the associated version of cromwell
     */
    public Map<String, String> getEngineVersions() {
        Map<String, String> engineVersions = new HashMap<>();
        CromwellVersion version = client.getVersion();
        engineVersions.put("cromwell", version.getCromwell());
        return engineVersions;
    }


    public RunListResponse listRuns(Integer pageSize, String pageToken) {

        CromwellSearch search = new CromwellSearch();
        if (pageToken != null) {
            String urlEncodedToken = new String(Base64.getDecoder().decode(pageToken), Charset.defaultCharset());
            String decodedToken = URLDecoder.decode(urlEncodedToken, Charset.defaultCharset());
            Pattern pattern = Pattern.compile("page=(?<page>[0-9]+)&pageSize=(?<pageSize>[0-9]+)");
            Matcher matcher = pattern.matcher(decodedToken);
            if (matcher.find()) {
                search.setPage(Long.valueOf(matcher.group("page")));
                search.setPageSize(Long.valueOf(matcher.group("pageSize")));
            }
        }

        if (search.getPage() == null) {
            search.setPage(Constants.DEFAULT_PAGE);
        }

        if (search.getPageSize() == null) {
            search.setPageSize(pageSize == null ? Constants.DEFAULT_PAGE_SIZE : pageSize);
        }

        CromwellResponse response = client.listWorkflows(search);
        RunListResponse runListResponse = CromwellWesMapper.mapCromwellResponseToRunListResposne(response);
        if (response.getTotalResultsCount() > search.getPage() * search.getPageSize()) {
            String urlEncodedNextPage = URLEncoder
                .encode(String.format("page=%d&pageSize=%d", search.getPage() + 1, search.getPageSize()), Charset
                    .defaultCharset());
            String base64NextPage = new String(Base64.getEncoder().encode(urlEncodedNextPage.getBytes()), Charset
                .defaultCharset());
            runListResponse.setNextPageToken(base64NextPage);
        }

        return runListResponse;
    }

    public RunLog getRun(String runId) {
        CromwellMetadataResponse metadataResponse = client.getMetadata(runId);
        return CromwellWesMapper.mapMetadataToRunLog(metadataResponse);

    }

    public RunStatus getRunStatus(String runId) {
        CromwellStatus status = client.getStatus(runId);
        return CromwellWesMapper.mapCromwellStatusToRunStatus(status);
    }

    public RunId cancel(String runId) {
        client.abortWorkflow(runId);
        return RunId.builder().id(runId).build();
    }

    public RunId execute(RunRequest runRequest) {
        try {
            Path tempDirectory = null;
            try {
                tempDirectory = Files.createTempDirectory("wes-dependency-resolver");
                CromwellExecutionRequest executionRequest = new CromwellExecutionRequest();

                if (runRequest.getWorkflowAttachments() == null) {
                    runRequest.setWorkflowAttachments(new MultipartFile[0]);
                }

                if (isUrl(runRequest.getWorkflowUrl())) {
                    executionRequest.setWorkflowUrl(runRequest.getWorkflowUrl());
                } else {
                    setWorkflowSourceAndDependencies(tempDirectory, runRequest, executionRequest);
                }
                executionRequest.setLabels(runRequest.getTags());
                WdlFileProcessor processor = setWorkflowInputs(runRequest, executionRequest);
                setWorkflowOptions(runRequest, executionRequest, processor);
                executionRequest.setWorkflowOnHold(processor != null && processor.requiresTransfer());

                CromwellStatus status = client.createWorkflow(executionRequest);

                if (processor != null && processor.requiresTransfer()) {
                    transferFilesAsync(processor, status);
                }

                return RunId.builder().id(status.getId()).build();
            } finally {
                if (tempDirectory != null && tempDirectory.toFile().exists()) {
                    Files.walk(tempDirectory.toAbsolutePath()).sorted(Comparator.reverseOrder()).map(Path::toFile)
                        .forEach(File::delete);
                }
            }
        } catch (IOException e) {
            throw new InvalidRequestException(e.getMessage(), e);
        } catch (FeignException e) {
            log.error(e.contentUTF8());
            throw new InvalidRequestException(e.getMessage(), e);
        }
    }

    private void setWorkflowOptions(RunRequest runRequest, CromwellExecutionRequest cromwellExecutionRequest, WdlFileProcessor processor) throws IOException {
        Map<String, String> engineParams = runRequest.getWorkflowEngineParameters();
        Map<String, Object> cromwellOptions = new HashMap<>();
        Optional<MultipartFile> optionsFile = Stream.of(runRequest.getWorkflowAttachments())
            .filter((file) -> file.getOriginalFilename().endsWith("options.json")).findFirst();
        TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {
        };

        if (optionsFile.isPresent()) {
            Map<String, Object> optionsJson = mapper
                .readValue(optionsFile.get().getInputStream(), typeReference);

            if (optionsJson != null) {
                cromwellOptions.putAll(optionsJson);
            }
        }

        if (runRequest.getWorkflowEngineParameters() != null && !runRequest.getWorkflowEngineParameters().isEmpty()) {
            JsonNodeFactory nodeFactory = new JsonNodeFactory(false);
            ObjectNode objectNode = nodeFactory.objectNode();
            for (Entry<String, String> entry : engineParams.entrySet()) {
                JsonNode node = extractJsonNode(entry.getValue());
                objectNode.set(entry.getKey(), node);
            }
            cromwellOptions.putAll(mapper.readValue(mapper.treeAsTokens(objectNode), typeReference));
        }

        if (processor != null && !processor.getMappedFiles().isEmpty()) {
            cromwellOptions.put(Constants.ORIGINAL_FILE_OBJECT_MAPPING, processor.getMappedFiles());
        }

        cromwellExecutionRequest.setWorkflowOptions(cromwellOptions);
    }

    private JsonNode extractJsonNode(String value) throws IOException {
        try {
            return mapper.readTree(value);
        } catch (JsonParseException parseException) {
            return new TextNode(value);
        }
    }

    private void setWorkflowSourceAndDependencies(Path tempDirectory, RunRequest runRequest, CromwellExecutionRequest cromwellRequest) throws IOException {
        if (runRequest.getWorkflowAttachments() == null || runRequest.getWorkflowAttachments().length == 0) {
            throw new InvalidRequestException("Url provided is relative however no workflowAttachments are defined");
        }

        for (MultipartFile file : runRequest.getWorkflowAttachments()) {
            if (file.getOriginalFilename() == null) {
                throw new InvalidRequestException("Unamed workflow attachment provided. All files must have a file name");
            }
        }

        cromwellRequest
            .setWorkflowSource(getSourceWdl(runRequest.getWorkflowUrl(), runRequest.getWorkflowAttachments()));
        if (runRequest.getWorkflowAttachments().length > 1) {

            Optional<MultipartFile> dependenciesZip = Stream.of(runRequest.getWorkflowAttachments())
                .filter(file -> file.getOriginalFilename().endsWith(Constants.DEPENDENCIES_FILE)).findFirst();
            if (dependenciesZip.isPresent()) {
                Path depPath = Paths.get(tempDirectory.toString(), Constants.DEPENDENCIES_FILE);
                File depFile = depPath.toFile();
                dependenciesZip.get().transferTo(depFile);
                cromwellRequest.setWorkflowDependencies(depFile);
            } else {
                List<MultipartFile> wdls = Stream.of(runRequest.getWorkflowAttachments())
                    .filter(file -> file.getOriginalFilename().endsWith(".wdl")).collect(Collectors.toList());
                cromwellRequest.setWorkflowDependencies(createDependenciesZip(tempDirectory, wdls));
            }

        }
        cromwellRequest.setWorkflowInputs(runRequest.getWorkflowParams());

    }

    private String getSourceWdl(String url, MultipartFile[] files) throws IOException {
        return new String(Stream.of(files).filter(file -> file.getOriginalFilename().equals(url)).findFirst()
            .orElseThrow(() ->
                new InvalidRequestException("Could not identify workflow source file")).getBytes(), Charset
            .defaultCharset());
    }

    private boolean isUrl(String workflowUrl) {
        try {
            new URL(workflowUrl);
            return true;
        } catch (MalformedURLException e) {
            return false;
        }
    }

    private File createDependenciesZip(Path tempDirectory, List<MultipartFile> files) throws IOException {
        Path depPath = Paths.get(tempDirectory.toString(), Constants.DEPENDENCIES_FILE);
        File depFile = depPath.toFile();
        FileOutputStream outputStream = new FileOutputStream(depFile);
        ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream);
        for (MultipartFile sourceFile : files) {
            Path path = Paths.get(tempDirectory.toString(), sourceFile.getOriginalFilename());
            File file = path.toFile();
            file.getParentFile().mkdirs();
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            fileOutputStream.write(sourceFile.getBytes());
            fileOutputStream.close();
        }

        writeFilesToZip(zipOutputStream, tempDirectory.toUri(), tempDirectory.toFile());
        zipOutputStream.close();
        return depFile;
    }

    private void writeFilesToZip(ZipOutputStream outputStream, URI base, File file) throws IOException {
        String path = base.relativize(file.toURI()).getPath();
        if (file.isDirectory()) {
            if (!path.endsWith("/")) {
                path = path + "/";
            }
            ZipEntry zipEntry = new ZipEntry(path);
            outputStream.putNextEntry(zipEntry);
            File[] listedFiles = file.listFiles();
            if (listedFiles != null) {
                for (File sub : listedFiles) {
                    writeFilesToZip(outputStream, base, sub);
                }
            }
            outputStream.closeEntry();
        } else {
            ZipEntry zipEntry = new ZipEntry(path);
            zipEntry.setSize(file.length());
            outputStream.putNextEntry(zipEntry);
            outputStream.write(Files.readAllBytes(file.toPath()));
            outputStream.closeEntry();
        }
    }


    private WdlFileProcessor setWorkflowInputs(RunRequest runRequest, CromwellExecutionRequest executionRequest) throws IOException {
        Map<String, Object> cromwellInputs = new HashMap<>();
        WdlFileProcessor processor = null;
        if (runRequest.getWorkflowParams() != null && !runRequest.getWorkflowParams().isEmpty()) {
            WdlValidationResponse schema = getWorkflowSchema(runRequest);
            processor = new WdlFileProcessor(runRequest.getWorkflowParams(), schema);
            processor.applyFileMapping(new FileMapper() {
                @Override
                public void map(FileWrapper wrapper) {

                }

                @Override
                public boolean shouldMap(FileWrapper wrapper) {
                    return false;
                }
            });
            cromwellInputs.putAll(processor.getProcessedInputs());
        }
        executionRequest.setWorkflowInputs(cromwellInputs);
        return processor;

    }

    private WdlValidationResponse getWorkflowSchema(RunRequest runRequest) throws IOException {
        WdlValidationRequest request = new WdlValidationRequest();
        if (isUrl(runRequest.getWorkflowUrl())) {
            request.setUri(request.getUri());
        } else {
            request.setUri(runRequest.getWorkflowUrl());
            List<WdlWorkflowDependency> dependencies = new ArrayList<>();
            List<MultipartFile> wdls = Stream.of(runRequest.getWorkflowAttachments())
                .filter(file -> file.getOriginalFilename().endsWith(".wdl")).collect(Collectors.toList());
            for (MultipartFile file : wdls) {
                WdlWorkflowDependency dependency = new WdlWorkflowDependency();
                dependency.setContent(new String(file.getBytes(), Charset.defaultCharset()));
                dependency.setUri(file.getOriginalFilename());
                dependencies.add(dependency);
            }
            request.setDependencies(dependencies);
        }
        return validatorClient.validate(request);
    }

    @Async
    public void transferFilesAsync(WdlFileProcessor fileProcessor, CromwellStatus status) {

    }
}
