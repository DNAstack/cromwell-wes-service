package com.dnastack.wes.service;

import com.dnastack.wes.Constants;
import com.dnastack.wes.client.CromwellClient;
import com.dnastack.wes.client.OAuthTokenCache;
import com.dnastack.wes.client.WdlValidatorClient;
import com.dnastack.wes.config.AppConfig;
import com.dnastack.wes.exception.AuthorizationException;
import com.dnastack.wes.exception.InvalidRequestException;
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
import com.dnastack.wes.security.AuthenticatedUser;
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
import java.util.Arrays;
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
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service layer for converting WES api calls into Cromwell API calls
 */
@Slf4j
@Service
public class CromwellService {

    private final static ObjectMapper mapper = new ObjectMapper();
    private final CromwellClient client;
    private final WdlValidatorClient validatorClient;
    private final OAuthTokenCache oAuthTokenCache;
    private final FileMappingStore fileMappingStore;
    private final DrsService drsService;
    private final AppConfig config;
    private final TransferService transferService;

    @Autowired
    CromwellService(OAuthTokenCache oAuthTokenCache, CromwellClient cromwellClient, WdlValidatorClient validatorClient,
        FileMappingStore fileMappingStore, AppConfig appConfig, DrsService drsService, TransferService transferService) {
        this.oAuthTokenCache = oAuthTokenCache;
        this.client = cromwellClient;
        this.validatorClient = validatorClient;
        this.drsService = drsService;
        this.config = appConfig;
        this.transferService = transferService;
        this.fileMappingStore = fileMappingStore;
    }


    /**
     * Retrieve as list of workflows from cromwell and aggregate them into a map of state counts. If multi tenant
     * support is enabled, the state counts will be limited to those submitted by the current principal
     */
    public Map<State, Integer> getSystemStateCounts() {
        CromwellResponse response;
        if (config.getEnableMultiTenantSupport()) {
            CromwellSearch search = new CromwellSearch();
            search
                .setLabel(Arrays.asList(String.format("%s:%s", Constants.USER_LABEL, AuthenticatedUser.getSubject())));
            response = client.listWorkflows(search);
        } else {
            response = client.listWorkflows();
        }

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


    /**
     * Retrieve a list of runs from cromwell and convert them into <code>RunListResponse</code>. The WES Specification
     * requires that paginated responses not change after an initial request has been made. This feature is not
     * supported by cromwell (or by many other databases for that matter), and is also not performant either.
     * Additionally, cromwell does not provide a mechanism for ordering the listed outputs, therefore all results are
     * listed from most recent to oldest. Because of this limitation, listing jobs does not provide a guarantee that the
     * list has not changed since the last time a user fetched results.
     *
     * @param pageSize The size of the page to return. If pageToken is provided, then page size will be ignored. If
     * neither the pageSize or pageToken are defined, then the default page size will be used. {@link
     * Constants#DEFAULT_PAGE_SIZE}
     * @param pageToken The page token is an encoded string defining the next page to retrieve from the cromwell server.
     * It is a base64 encoded query string containing the page size and the next page
     * @return List of runs with next page set.
     */
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

        String user = AuthenticatedUser.getSubject();
        if (config.getEnableMultiTenantSupport() && user != null) {
            search.setLabel(Arrays.asList(String.format("%s:%s", Constants.USER_LABEL, user)));
        } else if (config.getEnableMultiTenantSupport()) {
            throw new AuthorizationException("Listing of workflows is restricted to authorized users");
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

    /**
     * Get a Specific Run by retrieving the job metadata from cromwell.
     *
     * @param runId The cromwell id
     * @return a complete run log
     */
    public RunLog getRun(String runId) {
        authorizeUserForRun(runId);
        CromwellMetadataResponse metadataResponse = client.getMetadata(runId);
        Map<String, Object> mappedFileObject = fileMappingStore.getMapping(runId);
        return CromwellWesMapper.mapMetadataToRunLog(metadataResponse, mappedFileObject);
    }

    /**
     * Get a Specific Run by retrieving the job status from cromwell.
     *
     * @param runId The cromwell id
     * @return a run status
     */
    public RunStatus getRunStatus(String runId) {
        authorizeUserForRun(runId);
        CromwellStatus status = client.getStatus(runId);
        return CromwellWesMapper.mapCromwellStatusToRunStatus(status);
    }

    /**
     * Attempt to cancel a workflow in cromwell if the job is in a "cancellable" state
     *
     * @param runId The cromwell id
     * @return a complete run log
     */
    public RunId cancel(String runId) {
        authorizeUserForRun(runId);
        client.abortWorkflow(runId);
        return RunId.builder().runId(runId).build();
    }


    /**
     * Given the user submitted run request, compose a new request to cromwell for executing a workflow. The run request
     * should contain all of the required information for creating a run.
     *
     * <h3>Workflow Files</h3>
     * <p/>
     * In a WES call, any workflow descriptor files are contained directly in the request in the form of
     * <code>workflow_attachments</code>. This flat array of files does correspond to how cromwell expects to receive a
     * workflowSource file or its dependencies and therefore must be mapped in th following ways:
     * <ul>
     * <li><strong>workflowUrl:</strong> If the WES request points to an external URL, this will be passed directly to
     * cromwell</li>
     * <li><strong>workflowSource:</strong> If the WES request points to a relative URL contained in the
     * <code>workflow_attachments</code> then the file referenced by the <code>workflow_url</code> will be read as a
     * <code>UTF-8</code> string and attached to the cromwell request as the main source file</li>
     * <li><strong>workflowDependencies:</strong> WES has a different approach to handling dependencies then cromwell
     * does. In WES all files are submitted as <code>workflow_attachments</code>however with cromwell a distinct zip
     * file containing the dependencies is what is expected. If a <code>RunRequest</code> contains more then a single
     * <code>WDL</code> file, then any additional file will be bundled in a Zip File and submitted to cromwell. Source
     * files will be added to the zip folder according to their submitted file names. For example, if two dependencies
     * are submitted with the names <code>lib/additional_tasks.wdl</code> and <code>lib/struct/additional_structs.wdl</code>
     * the resulting zip file will contain entries according to the following:
     * <pre>
     * |-lib/
     *   |- additional_tasks.wdl
     *   |- struct/
     *      |- additional_structs.wdl
     * </pre>
     * <p>
     * The user can provide a dependencies file as a <code>workflow_attachment</code> named {@link
     * Constants#DEPENDENCIES_FILE} which will be sent to cromwell instead of the zip file produced by this method.
     * </li>
     * <li><strong>workflowOptions:</strong> Workflow options can be provided in two different ways.
     * <ol>
     * <li>A file submitted as a <code>workflow_attachment</code> with the name defined by {@link
     * Constants#OPTIONS_FILE}. This file is expected to be <code>JSON</code> and will be extracted into a Hashmap and
     * merged with the options submitted by <code>engine_parameters</code></li>
     * <li>Key-Value pairs provided by the {@link RunRequest#workflowEngineParameters}. The values will extracted as
     * <code>JSON</code> before being merged in the options map.
     * </li>
     * </ol>
     * </li>
     * <li><strong>workflowInputs</strong> The WES inputs will be expected to be in the same format which cromwell
     * accepts. Any files that are provided as <code>DRS</code> objects will be expected to be mapped into a resolvable
     * URL that will work with the specific cromwell backend. Additionally, if the object transfer service is configured
     * then, files will be mapped into their final destination and then localized by the object transfer service.
     * </ul>
     */
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
                WdlFileProcessor processor = setWorkflowInputs(runRequest, executionRequest);

                if (processor != null) {
                    transferService.configureObjectsForTransfer(processor.getMappedObjects(), runRequest);
                }

                setWorkflowLabels(runRequest, executionRequest);
                setWorkflowOptions(runRequest, executionRequest);
                executionRequest.setWorkflowOnHold(processor != null && processor.requiresTransfer());
                CromwellStatus status = client.createWorkflow(executionRequest);
                RunId runId = RunId.builder().runId(status.getId()).build();

                if (processor != null) {
                    if (processor.requiresTransfer()) {
                        transferService
                            .transferFiles(new TransferContext(runId
                                .getRunId(), processor, null, this::transferCallBack));
                    }

                    Map<String, Object> mappedFiles = processor.getMappedFiles();
                    fileMappingStore.saveMapping(status.getId(), mappedFiles);
                }

                return runId;
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
            log.error(e.getMessage(), e);
            throw new InvalidRequestException(e.getMessage(), e);
        }
    }


    private void authorizeUserForRun(String runId) {
        String user = AuthenticatedUser.getSubject();
        if (config.getEnableMultiTenantSupport()) {
            CromwellSearch search = new CromwellSearch();
            search.setId(Arrays.asList(runId));
            search.setLabel(Arrays.asList(String.format("%s:%s", Constants.USER_LABEL, user)));
            CromwellResponse response = client.listWorkflows(search);
            if (response.getTotalResultsCount() == 0) {
                throw new AuthorizationException("The resource does not exist or the user is unauthorized to access "
                    + "it");
            }
        }
    }

    private void transferCallBack(Throwable throwable, String runId) {
        if (throwable != null) {
            log.error("Encountered error while transferring files. Aborting run {}", runId);
            client.abortWorkflow(runId);
        } else {
            log.info("Transferring files complete, releasing hold on run {}", runId);
            client.releaseHold(runId);
        }
    }

    private void setWorkflowLabels(RunRequest runRequest, CromwellExecutionRequest cromwellExecutionRequest) {
        Map<String, String> labels = new HashMap<>();
        if (runRequest.getTags() != null) {
            labels.putAll(runRequest.getTags());
        }

        String user = AuthenticatedUser.getSubject();
        if (user != null) {
            labels.put(Constants.USER_LABEL, user);
        }
        cromwellExecutionRequest.setLabels(labels);
    }

    private void setWorkflowOptions(RunRequest runRequest, CromwellExecutionRequest cromwellExecutionRequest) throws IOException {
        Map<String, String> engineParams = runRequest.getWorkflowEngineParameters();
        Map<String, Object> cromwellOptions = new HashMap<>();
        Optional<MultipartFile> optionsFile = Stream.of(runRequest.getWorkflowAttachments())
            .filter((file) -> file.getOriginalFilename().endsWith(Constants.OPTIONS_FILE)).findFirst();
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
                if (Constants.VALID_CROMWELL_OPTIONS.contains(entry.getKey())) {
                    JsonNode node = extractJsonNode(entry.getValue());
                    objectNode.set(entry.getKey(), node);
                }
            }
            cromwellOptions.putAll(mapper.readValue(mapper.treeAsTokens(objectNode), typeReference));
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
            processor = new WdlFileProcessor(runRequest.getWorkflowParams(), schema, Arrays.asList(drsService));
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
        return validatorClient.validate(request, oAuthTokenCache.getToken().getToken());
    }

}
