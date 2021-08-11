package com.dnastack.wes.transfer;

import com.dnastack.wes.cromwell.CromwellClient;
import com.dnastack.wes.shared.CredentialsModel;
import com.dnastack.wes.shared.ServiceAccountException;
import com.dnastack.wes.wdl.ObjectWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.auth.oauth2.GoogleCredentials;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.jdbi.v3.core.Jdbi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.lang.String.format;

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


    public void transferFiles(String subject, TransferContext context) {
        if (config.isEnabled()) {
            try {
                performTransfer(subject, context);
            } catch (RuntimeException e) {
                try {
                    cromwellClient.abortWorkflow(context.runId);
                } catch (RuntimeException e1) {
                    e.addSuppressed(e1);
                }

                try {
                    throw e;
                } catch (FeignException fe) {
                    throw new TransferFailedException(format("Failed to start file transfer on run [%s] with status %d: %s",
                        context.getRunId(),
                        fe.status(),
                        fe.contentUTF8()),
                        fe);
                } catch (ServiceAccountException sae) {
                    throw new TransferFailedException(format("Error authorizing for object transfer on run [%s]: %s",
                        context.getRunId(),
                        sae.getMessage()),
                        sae);
                } catch (RuntimeException re) {
                    throw new TransferFailedException(format("Failed to start file transfer on run [%s]: %s",
                        context.getRunId(),
                        re.getMessage()),
                        re);
                }
            }
        }
    }


    private void performTransfer(String subject, TransferContext context) {
        Map<String, TransferRequest> transferRequests = new HashMap<>();
        List<TransferSpec> objectsToTransfer = context.getObjectsToTransfer();
        for (TransferSpec transferSpec : objectsToTransfer) {
            String srcAccessToken = transferSpec.getSourceSpec().getAccessToken();
            TransferRequest request = transferRequests.computeIfAbsent(srcAccessToken, (k) -> {
                return TransferRequest.builder()
                    .srcAccessKeyId(transferSpec.getSourceSpec().getAccessKeyId())
                    .srcAccessToken(srcAccessToken)
                    .srcSessionToken(transferSpec.getSourceSpec().getSessionToken())
                    .dstAccessKeyId(transferSpec.getTargetSpec().getAccessKeyId())
                    .dstAccessToken(transferSpec.getTargetSpec().getAccessToken())
                    .dstSessionToken(transferSpec.getTargetSpec().getSessionToken())
                    .build();
            });
            String source = transferSpec.getSourceSpec().getUri();
            String destination = transferSpec.getTargetSpec().getUri();

            request.getCopyPairs().add(Arrays.asList(source, destination));
        }

        List<TransferJob> transfersToMonitor = new ArrayList<>();
        for (TransferRequest request : transferRequests.values()) {
            TransferJob job = transferServiceClientFactory.getClient()
                .submitTransferRequest(request, subject);
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

    public List<TransferSpec> configureObjectsForTransfer(
        List<ObjectWrapper> objectWrappers,
        Map<String, CredentialsModel> objectAccessCredentials
    ) {
        List<TransferSpec> objectsToTransfer = new ArrayList<>();
        if (config.isEnabled() && objectWrappers != null && !objectWrappers.isEmpty()) {
            log.trace("Configuring object transfers for {} objects", objectWrappers.size());
            String stagingDirectoryPrefix = RandomStringUtils.randomAlphanumeric(6);
            for (ObjectWrapper wrapper : objectWrappers) {
                configureObjectForTransfer(stagingDirectoryPrefix, objectAccessCredentials, wrapper)
                    .forEach(transferSpec -> {
                        objectsToTransfer.add(transferSpec);
                        updateObjectWrapper(wrapper, transferSpec.getTargetSpec().getUri());
                    });
            }
        }

        return objectsToTransfer;
    }

    private Stream<TransferSpec> configureObjectForTransfer(
        String stagingDirectoryPrefix,
        Map<String, CredentialsModel> objectAccessCredentials,
        ObjectWrapper objectWrapper
    ) {
        if (config.isEnabled()) {
            JsonNode mappedValue = objectWrapper.getMappedValue();
            return configureNodeForTransfer(stagingDirectoryPrefix, objectAccessCredentials, mappedValue, objectWrapper);
        } else {
            return Stream.of();
        }
    }


    private Stream<TransferSpec> configureNodeForTransfer(
        String staginDirectoryPrefix,
        Map<String, CredentialsModel> objectAccessCredentials,
        JsonNode node,
        ObjectWrapper objectWrapper
    ) {
        if (node.isTextual()) {
            String objectToTransfer = node.textValue();
            if (!config.getObjectPrefixWhitelist().stream().anyMatch(objectToTransfer::startsWith)) {
                try {
                    URI objectToTransferUri = new URI(objectToTransfer);
                    if (objectAccessCredentials.containsKey(objectToTransfer)) {
                        CredentialsModel sourceCredentials = objectAccessCredentials.get(objectToTransfer);
                        return getTransferSpecs(sourceCredentials, objectToTransfer, objectToTransferUri, staginDirectoryPrefix);
                    } else {
                        String hostUri = objectToTransferUri.getScheme() + "://" + objectToTransferUri.getHost();
                        if (objectAccessCredentials.containsKey(hostUri)) {
                            CredentialsModel sourceCredentials = objectAccessCredentials.get(hostUri);
                            return getTransferSpecs(sourceCredentials, objectToTransfer, objectToTransferUri, staginDirectoryPrefix);
                        }
                    }
                } catch (URISyntaxException e) {
                    // Silently handle this
                }
            }
        } else if (node.isArray()) {
            ArrayNode arrayNode = (ArrayNode) node;
            return StreamSupport.stream(arrayNode.spliterator(), false)
                .flatMap(itemNode -> configureNodeForTransfer(staginDirectoryPrefix, objectAccessCredentials, itemNode, objectWrapper));
        } else if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            return StreamSupport.stream(Spliterators.spliteratorUnknownSize(objectNode.fields(), 0), false)
                .flatMap(entry -> configureNodeForTransfer(staginDirectoryPrefix, objectAccessCredentials, entry.getValue(), objectWrapper));
        }

        return Stream.of();
    }

    private Stream<TransferSpec> getTransferSpecs(
        CredentialsModel sourceCredentials,
        String objectToTransfer,
        URI objectToTransferUri,
        String staginDirectoryPrefix
    ) {
        String destination = generateTransferDestination(staginDirectoryPrefix, objectToTransferUri);
        String dstAccessToken = getDestinationAccessToken(destination).orElse(null);

        return Stream.of(TransferSpec.builder()
            .sourceSpec(new TransferSpec.BlobSpec(
                sourceCredentials.getAccessKeyId(),
                sourceCredentials.getAccessToken(),
                sourceCredentials.getSessionToken(),
                objectToTransfer
            ))
            .targetSpec(new TransferSpec.BlobSpec(
                null,
                dstAccessToken,
                null,
                destination
            ))
            .build());
    }

    private Optional<String> getDestinationAccessToken(String destination) {
        if (!destination.startsWith(config.getStagingDirectory())) {
            log.warn("Access token for destination [{}] outside staging directory [{}] may not be be authorized", destination, config.getStagingDirectory());
        }

        if (destination.startsWith("gs://")) {
            try {
                return Optional.of(getOrRefreshGcpAccessToken());
            } catch (IOException e) {
                log.debug("Unable to get GCP access token: {}", e.getMessage());
                log.trace("Detailed info on GCP access token exception", e);

                return Optional.empty();
            }
        } else {
            return Optional.empty();
        }
    }

    private String getOrRefreshGcpAccessToken() throws IOException {
        final GoogleCredentials credentials = getGcpCredentials();
        credentials.refreshIfExpired();

        return credentials.getAccessToken().getTokenValue();
    }

    private GoogleCredentials getGcpCredentials() throws IOException {
        final GoogleCredentials credentials = GoogleCredentials.getApplicationDefault();
        if (credentials.createScopedRequired()) {
            return credentials.createScoped(List.of("https://www.googleapis.com/auth/cloud-platform"));
        } else {
            return credentials;
        }
    }

    /**
     * Modifies objectWrapper in-place with mapped path for file-transfer.
     */
    private void updateObjectWrapper(ObjectWrapper objectWrapper, String destination) {
        objectWrapper.setTransferDestination(destination);
        objectWrapper.setWasMapped(true);
        if (objectWrapper.getMappedValue() != null) {
            objectWrapper.setSourceDestination(objectWrapper.getMappedValue().asText());
        }
        objectWrapper.setMappedValue(new TextNode(destination));
        objectWrapper.setRequiresTransfer(true);
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
