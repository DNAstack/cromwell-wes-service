package com.dnastack.wes.service;

import com.dnastack.wes.client.TransferServiceClient;
import com.dnastack.wes.config.TransferConfig;
import com.dnastack.wes.model.transfer.TransferJob;
import com.dnastack.wes.model.transfer.TransferRequest;
import com.dnastack.wes.security.AuthenticatedUser;
import com.dnastack.wes.wdl.ObjectWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import feign.FeignException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Service
public class TransferService {

    private final TransferServiceClient transferServiceClient;
    private final TransferConfig config;
    private final TaskScheduler taskScheduler;
    private final BlockingQueue<TransferContext> monitorQueue;


    @Autowired
    TransferService(TransferServiceClient transferServiceClient, TaskScheduler taskScheduler, TransferConfig config) {
        this.transferServiceClient = transferServiceClient;
        this.taskScheduler = taskScheduler;
        this.config = config;
        this.monitorQueue = new LinkedBlockingQueue<>();
    }

    @PostConstruct
    public void init() {
        if (config.isEnabled()) {
            for (int i = 0; i < config.getNumMonitoringThreads(); i++) {
                taskScheduler.scheduleWithFixedDelay(this::monitorTransfer, 13_000L);
            }
        }
    }


    private void monitorTransfer() {
        log.trace("Polling for queued transfer jobs");
        TransferContext context = monitorQueue.poll();
        if (context != null) {
            try {
                log.trace("Retrieved transfer context for run {}", context.getRunId());
                List<TransferJob> jobs = context.getTransferJobs();
                for (TransferJob job : jobs) {
                    if (!job.isDone()) {
                        log.trace("Updating transfer job {} state", job.getJobId());
                        TransferJob updatedJob = transferServiceClient.getTransferJob(job.getJobId());
                        job.setFileStatus(updatedJob.getFileStatus());
                    }
                }

                if (jobs.stream().allMatch(TransferJob::isDone)) {
                    if (jobs.stream().anyMatch(job -> !job.isSuccessful())) {
                        log.error("One or more transfer jobs for run {} failed", context.getRunId());
                        throw new TransferFailedException("Could not complete transfer for run " + context.runId);
                    } else {
                        log.info("Successfully completed transfer for run {}", context.getRunId());
                        context.complete();
                    }
                } else {
                    log.debug("One or more transfer jobs is still executing for run {}, adding job back to queue", context
                        .getRunId());
                    monitorQueue.add(context);
                }

            } catch (FeignException e) {
                log.error(e.contentUTF8());
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                context.fail(e);
            }
        }
    }

    public void transferFiles(TransferContext context) {
        try {
            if (config.isEnabled()) {
                try {
                    performTransfer(context);
                } catch (Exception e) {
                    context.callback.callAfterTransfer(e, context.runId);
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }


    private void performTransfer(TransferContext context) {
        Map<String, TransferRequest> transferRequests = new HashMap<>();
        Map<String, String[]> objectsToTransfer = context.getObjectsToTransfer();
        for (Map.Entry<String, String[]> entry : objectsToTransfer.entrySet()) {

            if (entry.getValue().length != 2) {
                throw new IllegalArgumentException("Object to transfer must have a destination and an access token");
            }

            String source = entry.getKey();
            String destination = entry.getValue()[0];
            String accessToken = entry.getValue()[1];
            TransferRequest request;
            request = transferRequests.computeIfAbsent(accessToken, (k) -> new TransferRequest(accessToken));
            List<String> copyPair = Arrays.asList(source, destination);
            request.getCopyPairs().add(copyPair);
        }


        List<TransferJob> transfersToMonitor = new ArrayList<>();
        for (TransferRequest request : transferRequests.values()) {
            TransferJob job = transferServiceClient.submitTransferRequest(request, AuthenticatedUser.getSubject());
            transfersToMonitor.add(job);
        }
        log.info("Submitting {} transfer jobs for run {}", transfersToMonitor.size(), context.getRunId());
        context.setTransferJobs(transfersToMonitor);
        log.trace("Adding transfer jobs to monitoring queue");
        monitorQueue.add(context);
    }


    public Map<String, String[]> configureObjectsForTransfer(List<ObjectWrapper> objectWrappers, Map<String, String> objectAccessTokens) {
        Map<String, String[]> objectsToTransfer = new HashMap<>();
        if (config.isEnabled() && objectWrappers != null && !objectWrappers.isEmpty()) {
            log.trace("Configuring object transfers for {} objects", objectWrappers.size());
            String stagingDirectoryPrefix = RandomStringUtils.randomAlphanumeric(6);
            for (ObjectWrapper wrapper : objectWrappers) {
                configureObjectForTransfer(stagingDirectoryPrefix, objectsToTransfer, objectAccessTokens, wrapper);
            }
        }
        return objectsToTransfer;
    }

    private void configureObjectForTransfer(String stagingDirectoryPrefix, Map<String, String[]> objectsToTransfer, Map<String, String> objectAccessTokens,
        ObjectWrapper objectWrapper) {
        if (config.isEnabled()) {
            JsonNode mappedValue = objectWrapper.getMappedvalue();
            configureNodeForTransfer(stagingDirectoryPrefix, objectsToTransfer, objectAccessTokens, mappedValue);
        }
    }


    private void configureNodeForTransfer(String staginDirectoryPrefix, Map<String, String[]> objectsToTransfer, Map<String, String> objectAccessTokens, JsonNode node) {
        if (node.isTextual()) {
            String objectToTransfer = node.textValue();
            if (!config.getObjectPrefixWhitelist().stream().anyMatch(objectToTransfer::startsWith)) {
                try {
                    URI objectToTransferUri = new URI(objectToTransfer);
                    if (objectAccessTokens.containsKey(objectToTransfer)) {
                        String accessToken = objectAccessTokens.get(objectToTransfer);
                        String destination = generateTransferDestination(staginDirectoryPrefix, objectToTransferUri);
                        objectsToTransfer.put(objectToTransfer, new String[]{destination, accessToken});
                        return;
                    } else {
                        String hostUri = objectToTransferUri.getScheme() + "://" + objectToTransferUri.getHost();
                        if (objectAccessTokens.containsKey(hostUri)) {
                            String accessToken = objectAccessTokens.get(hostUri);
                            String destination = generateTransferDestination(staginDirectoryPrefix, objectToTransferUri);
                            objectsToTransfer.put(objectToTransfer, new String[]{destination, accessToken});
                        }
                    }
                } catch (URISyntaxException e) {
                    // Silently handle thisl
                }
            }
        } else if (node.isArray()) {
            ArrayNode arrayNode = (ArrayNode) node;
            arrayNode.forEach((itemNode) -> {
                configureNodeForTransfer(staginDirectoryPrefix, objectsToTransfer, objectAccessTokens, itemNode);
            });
        } else if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            objectNode.fields().forEachRemaining(entry -> {
                configureNodeForTransfer(staginDirectoryPrefix, objectsToTransfer, objectAccessTokens, entry
                    .getValue());
            });
        }
    }

    private String generateTransferDestination(String prefix, URI originalUri) {
        String stagingDirectory = config.getStagingDirectory();
        String path = originalUri.getPath();

        if (originalUri.getHost() != null) {
            prefix = prefix + "/" + originalUri.getHost();
        }

        return UriComponentsBuilder.fromUriString(stagingDirectory).path(prefix).path(path).build().toString();
    }

}
