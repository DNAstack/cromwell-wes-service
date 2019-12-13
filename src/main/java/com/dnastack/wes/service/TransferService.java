package com.dnastack.wes.service;

import com.dnastack.wes.client.CromwellClient;
import com.dnastack.wes.client.TransferServiceClientFactory;
import com.dnastack.wes.config.TransferConfig;
import com.dnastack.wes.data.TrackedTransfer;
import com.dnastack.wes.data.TrackedTransferDao;
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
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.jdbi.v3.core.Jdbi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Service
public class TransferService {

    private final TransferServiceClientFactory transferServiceClientFactory;
    private final TransferConfig config;
    private final Jdbi jdbi;
    private final CromwellClient cromwellClient;


    @Autowired
    TransferService(TransferServiceClientFactory transferServiceClientFactory, CromwellClient cromwellClient, Jdbi jdbi, TransferConfig config) {
        this.transferServiceClientFactory = transferServiceClientFactory;
        this.config = config;
        this.cromwellClient = cromwellClient;
        this.jdbi = jdbi;
    }


    @Scheduled(initialDelay = 30_000, fixedDelay = 30_000)
    public void monitorTransfers() {
        List<TrackedTransfer> trackedTransfers = jdbi
            .withExtension(TrackedTransferDao.class, TrackedTransferDao::getTransfers);

        if (trackedTransfers != null) {
            for (TrackedTransfer trackedTransfer : trackedTransfers) {
                try {
                    ZonedDateTime now = ZonedDateTime.now();
                    ZonedDateTime created = trackedTransfer.getCreated();

                    if (now.minus(config.getMaxTransferWaitTimeMs(), ChronoUnit.MILLIS).isAfter(created)) {
                        throw new TransferFailedException("Transfer for job " + trackedTransfer.getCromwellId()
                            + " has exceeded the maximum wait time. Failing the run and considering the transfer to have failed");
                    }
                    log.trace("Retrieved transfer context for run {}", trackedTransfer.getCromwellId());
                    List<TransferJob> jobs = new ArrayList<>();
                    for (String jobId : trackedTransfer.getTransferJobIds()) {
                        log.trace("Updating transfer job {} state", jobId);
                        TransferJob updatedJob = transferServiceClientFactory.getClient().getTransferJob(jobId);
                        jobs.add(updatedJob);
                    }

                    if (jobs.stream().allMatch(TransferJob::isDone)) {
                        if (jobs.stream().anyMatch(job -> !job.isSuccessful())) {
                            log.error("One or more transfer jobs for run {} failed", trackedTransfer.getCromwellId());
                            throw new TransferFailedException(
                                "Could not complete transfer for run " + trackedTransfer.getCromwellId());
                        } else {
                            transferSucceeded(trackedTransfer);
                        }
                    } else {
                        setLastUpdate(trackedTransfer);
                    }
                } catch (Exception e) {
                    transferFailed(e, trackedTransfer);
                }
            }

        }
    }

    private void transferSucceeded(TrackedTransfer transfer) {
        cromwellClient.releaseHold(transfer.getCromwellId());
        jdbi.withExtension(TrackedTransferDao.class, dao -> {
            dao.removeTrackedTransfer(transfer.getCromwellId());
            return null;
        });
    }

    private void transferFailed(Throwable e, TrackedTransfer transfer) {
        log.error(e.getMessage(), e);

        if (e instanceof FeignException) {
            if (transfer.getFailureAttempts() >= config.getMaxMonitoringFailures()) {
                String message = "The number of failure attempts has exceeded the maximum allowed for run: " + transfer
                    .getCromwellId();
                transferFailed(new TransferFailedException(message), transfer);
            } else {
                jdbi.withExtension(TrackedTransferDao.class, dao -> {
                    dao.updateTransfeFailureAttempts(ZonedDateTime.now(),
                        transfer.getFailureAttempts() + 1, transfer.getCromwellId());
                    return null;
                });
            }
        } else {
            cromwellClient.abortWorkflow(transfer.getCromwellId());
            jdbi.withExtension(TrackedTransferDao.class, dao -> {
                dao.removeTrackedTransfer(transfer.getCromwellId());
                return null;
            });
        }
    }

    private void setLastUpdate(TrackedTransfer transfer) {
        jdbi.withExtension(TrackedTransferDao.class, dao -> {
            dao.updateTransfer(ZonedDateTime.now(), transfer.getCromwellId());
            return null;
        });
    }


    public void transferFiles(TransferContext context) {
        try {
            if (config.isEnabled()) {
                try {
                    performTransfer(context);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                    cromwellClient.abortWorkflow(context.runId);
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
            TransferJob job = transferServiceClientFactory.getClient()
                .submitTransferRequest(request, AuthenticatedUser.getSubject());
            transfersToMonitor.add(job);
        }
        log.info("Submitting {} transfer jobs for run {}", transfersToMonitor.size(), context.getRunId());
        context.setTransferJobs(transfersToMonitor);
        log.trace("Adding transfer jobs to monitoring queue");
        saveTransferJob(context);
    }


    private void saveTransferJob(TransferContext context) {
        jdbi.withExtension(TrackedTransferDao.class, dao -> {
            TrackedTransfer transfer = new TrackedTransfer();
            transfer.setCromwellId(context.getRunId());
            transfer.setTransferJobIds(context.getTransferJobs().stream().map(TransferJob::getJobId)
                .collect(Collectors.toList()));
            transfer.setLastUpdate(ZonedDateTime.now());
            transfer.setFailureAttempts(0);
            transfer.setCreated(ZonedDateTime.now());
            dao.saveTrackedTransfer(transfer);
            return null;
        });

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
