package com.dnastack.wes.cromwell;

import com.dnastack.wes.AppConfig;
import com.dnastack.wes.Constants;
import com.dnastack.wes.api.*;
import com.dnastack.wes.drs.DrsObjectResolverFactory;
import com.dnastack.wes.security.AuthenticatedUser;
import com.dnastack.wes.shared.AuthorizationException;
import com.dnastack.wes.shared.CredentialsModel;
import com.dnastack.wes.shared.InvalidRequestException;
import com.dnastack.wes.shared.NotFoundException;
import com.dnastack.wes.storage.BlobStorageClient;
import com.dnastack.wes.transfer.TransferContext;
import com.dnastack.wes.transfer.TransferService;
import com.dnastack.wes.transfer.TransferSpec;
import com.dnastack.wes.wdl.ObjectTranslator;
import com.dnastack.wes.wdl.WdlFileProcessor;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.jdbi.v3.core.Jdbi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Service layer for converting WES api calls into Cromwell API calls
 */
@Slf4j
@Service
public class CromwellService {

    private final static ObjectMapper mapper = new ObjectMapper();
    private final CromwellClient client;
    private final BlobStorageClient storageClient;
    private final Jdbi jdbi;
    private final DrsObjectResolverFactory drsObjectResolverFactory;
    private final AppConfig config;
    private final TransferService transferService;
    private final PathTranslatorFactory pathTranslatorFactory;

    @Autowired
    CromwellService(
        CromwellClient cromwellClient,
        BlobStorageClient storageClient,
        PathTranslatorFactory pathTranslatorFactory,
        DrsObjectResolverFactory drsObjectResolverFactory,
        Jdbi jdbi,
        AppConfig appConfig,
        TransferService transferService
    ) {
        this.client = cromwellClient;
        this.pathTranslatorFactory = pathTranslatorFactory;
        this.drsObjectResolverFactory = drsObjectResolverFactory;
        this.config = appConfig;
        this.transferService = transferService;
        this.jdbi = jdbi;
        this.storageClient = storageClient;
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
     * @param pageSize  The size of the page to return. If pageToken is provided, then page size will be ignored. If
     *                  neither the pageSize or pageToken are defined, then the default page size will be used. {@link
     *                  Constants#DEFAULT_PAGE_SIZE}
     * @param pageToken The page token is an encoded string defining the next page to retrieve from the cromwell server.
     *                  It is a base64 encoded query string containing the page size and the next page
     *
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
     *
     * @return a complete run log
     */
    public RunLog getRun(String runId) {
        authorizeUserForRun(runId);
        CromwellMetadataResponse metadataResponse = getMetadata(runId);
        Map<String, Object> mappedFileObject = jdbi.withExtension(OriginalInputsDao.class, dao -> {
            OriginalInputs inputs = dao.getInputs(runId);
            return inputs == null ? null : inputs.getMapping();
        });

        return CromwellWesMapper
            .mapMetadataToRunLog(metadataResponse, mappedFileObject, pathTranslatorFactory
                .getTranslatorsForOutputs());
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

    private CromwellMetadataResponse getMetadata(String runId) {
        try {
            return client.getMetadata(runId);
        } catch (FeignException e) {
            if (e.status() == 400 || e.status() == 404) {
                throw new NotFoundException("Workflow execution with run_id " + runId + " does not exist.");
            } else {
                throw e;
            }
        }
    }

    /**
     * Get a Specific Run by retrieving the job status from cromwell.
     *
     * @param runId The cromwell id
     *
     * @return a run status
     */
    public RunStatus getRunStatus(String runId) {
        authorizeUserForRun(runId);
        CromwellStatus status = getStatus(runId);
        return CromwellWesMapper.mapCromwellStatusToRunStatus(status);
    }

    private CromwellStatus getStatus(String runId) {
        try {
            return client.getStatus(runId);
        } catch (FeignException e) {
            if (e.status() == 400 || e.status() == 404) {
                throw new NotFoundException("Workflow execution with run_id " + runId + " does not exist.");
            } else {
                throw e;
            }
        }
    }

    /**
     * Attempt to cancel a workflow in cromwell if the job is in a "cancellable" state
     *
     * @param runId The cromwell id
     *
     * @return a complete run log
     */
    public RunId cancel(String runId) {
        authorizeUserForRun(runId);
        client.abortWorkflow(runId);
        return RunId.builder().runId(runId).build();
    }

    public void getLogBytes(OutputStream outputStream, String runId, String taskName, int index, String logKey) throws IOException {
        String logPath = getLogPath(runId, taskName, index, logKey);
        storageClient.readBytes(outputStream, logPath, null, null);
    }

    private String getLogPath(String runId, String taskName, int index, String logKey) throws IOException {
        CromwellMetadataResponse metadataResponse = getMetadata(runId);
        Map<String, List<CromwellTaskCall>> calls = metadataResponse.getCalls();
        if (calls == null) {
            throw new FileNotFoundException(
                "Could not read " + logKey + " for task " + taskName + "in run " + runId + ", it does not exist");
        } else if (!calls.containsKey(taskName) || calls.size() <= index) {
            throw new FileNotFoundException(
                "Could not read " + logKey + " for task " + taskName + "in run " + runId + ", it does not exist");
        }

        CromwellTaskCall taskCall = calls.get(taskName).get(index);
        if (logKey.equals("stderr")) {
            return taskCall.getStderr();
        } else {
            return taskCall.getStdout();
        }
    }

    public void getLogBytes(OutputStream outputStream, String runId) throws IOException {
        CromwellMetadataResponse response = client.getMetadata(runId);
        if (response.getFailures() != null) {
            ObjectMapper mapper = new ObjectMapper();
            outputStream.write(mapper.writeValueAsBytes(response.getFailures()));
        }
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
     * <li>Key-Value pairs provided by the . The values will extracted as
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
    public RunId execute(String subject, RunRequest runRequest) {
        try {
            Path tempDirectory = null;
            try {
                tempDirectory = Files.createTempDirectory("wes-dependency-resolver");
                CromwellExecutionRequest executionRequest = new CromwellExecutionRequest();
                final Map<String, Object> originalInputs = runRequest.getWorkflowParams();

                if (runRequest.getWorkflowAttachments() == null) {
                    runRequest.setWorkflowAttachments(new MultipartFile[0]);
                }

                if (isUrl(runRequest.getWorkflowUrl())) {
                    executionRequest.setWorkflowUrl(runRequest.getWorkflowUrl());
                } else {
                    setWorkflowSourceAndDependencies(tempDirectory, runRequest, executionRequest);
                }

                Map<String, CredentialsModel> credentials = getObjectAccessCredentials(runRequest);
                WdlFileProcessor processor = setWorkflowInputs(runRequest
                    .getWorkflowParams(), credentials, runRequest.getWorkflowAttachments(), executionRequest);

                uploadAttachments(runRequest, processor);

                List<TransferSpec> objectsToTransfer = null;
                if (processor != null) {
                    objectsToTransfer = transferService
                        .configureObjectsForTransfer(processor.getMappedObjects(), credentials);
                }

                setWorkflowLabels(runRequest, executionRequest);
                setWorkflowOptions(runRequest, executionRequest);
                executionRequest.setWorkflowOnHold(objectsToTransfer != null && !objectsToTransfer.isEmpty());
                CromwellStatus status = client.createWorkflow(executionRequest);
                RunId runId = RunId.builder().runId(status.getId()).build();

                jdbi.withExtension(OriginalInputsDao.class, dao -> {
                    dao.saveInputs(new OriginalInputs(runId.getRunId(), originalInputs));
                    return null;
                });

                if (objectsToTransfer != null && !objectsToTransfer.isEmpty()) {
                    transferService
                        .transferFiles(subject, new TransferContext(
                            runId.getRunId(),
                            objectsToTransfer,
                            null,
                            this::transferCallBack
                        ));

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

    private void uploadAttachments(RunRequest runRequest, WdlFileProcessor processor) throws IOException {
        String stagingFolder = UUID.randomUUID().toString();
        List<MultipartFile> attachmentFiles =
            Stream.of(runRequest.getWorkflowAttachments())
                .filter(attachment -> attachment.getOriginalFilename() != null)
                .filter(attachment -> !Constants.FILES_TO_IGNORE_FOR_STAGING.contains(attachment.getOriginalFilename()))
                .filter(attachment -> !attachment.getOriginalFilename().endsWith("wdl"))
                .collect(Collectors.toList());

        Map<String, String> mappedFiles = new HashMap<>();
        for (MultipartFile attachment : attachmentFiles) {
            String stagingBlobLocation = storageClient
                .writeBytes(attachment.getInputStream(), attachment.getSize(), stagingFolder, attachment.getOriginalFilename());
            mappedFiles.put(attachment.getOriginalFilename(), stagingBlobLocation);
        }

        processor.getMappedObjects()
            .stream().forEach(objectWrapper -> {
            JsonNode node = objectWrapper.getMappedValue();
            if (node instanceof TextNode && mappedFiles.containsKey(node.asText())) {
                objectWrapper.setMappedValue(new TextNode(mappedFiles.get(node.asText())));
            }
        });
    }

    private Map<String, CredentialsModel> getObjectAccessCredentials(RunRequest runRequest) throws IOException {
        Map<String, CredentialsModel> objectAccessCredentials;

        Optional<MultipartFile> objectAccessCredentialsFile =
            Stream.of(runRequest.getWorkflowAttachments())
                .filter(attachment -> attachment.getOriginalFilename() != null && attachment.getOriginalFilename()
                    .equals(Constants.OBJECT_ACCESS_CREDENTIALS_FILE)).findFirst();

        TypeReference<Map<String, CredentialsModel>> typeReference = new TypeReference<>() {
        };
        if (objectAccessCredentialsFile.isPresent()) {
            objectAccessCredentials = mapper
                .readValue(objectAccessCredentialsFile.get().getInputStream(), typeReference);
        } else {
            objectAccessCredentials = Collections.emptyMap();
        }
        return objectAccessCredentials;
    }

    private void transferCallBack(Throwable throwable, String runId) {
        if (throwable != null) {
            log.error("Encountered error while transferring files. Aborting run {}", runId);
            log.error(throwable.getMessage(), throwable);
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
        labels.put(Constants.WORKFLOW_URL_LABEL, runRequest.getWorkflowUrl());
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


    private WdlFileProcessor setWorkflowInputs(
        Map<String, Object> workflowParams,
        Map<String, CredentialsModel> credentials,
        MultipartFile[] uploadedAttachments,
        CromwellExecutionRequest executionRequest
    ) {
        Map<String, Object> cromwellInputs = new HashMap<>();
        WdlFileProcessor processor = null;
        Set<String> uploadFileNames = Stream.of(uploadedAttachments)
            .filter(attachment -> attachment.getOriginalFilename() != null)
            .map(MultipartFile::getOriginalFilename).collect(Collectors.toSet());
        if (workflowParams != null && !workflowParams.isEmpty()) {

            List<ObjectTranslator> translators = new ArrayList<>();
            translators.add(drsObjectResolverFactory.getService(credentials));
            translators.addAll(pathTranslatorFactory.getTranslatorsForInputs());
            processor = new WdlFileProcessor(workflowParams, uploadFileNames, translators);
            cromwellInputs.putAll(processor.getProcessedInputs());
        }
        executionRequest.setWorkflowInputs(cromwellInputs);
        return processor;
    }

}
