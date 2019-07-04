package com.dnastack.wes.transfer;

import com.dnastack.wes.AppConfig;
import com.dnastack.wes.client.ExternalAccountClient;
import com.dnastack.wes.client.TransferServiceClient;
import com.dnastack.wes.model.transfer.ExternalAccount;
import com.dnastack.wes.model.transfer.TransferJob;
import com.dnastack.wes.model.transfer.TransferRequest;
import com.dnastack.wes.security.AuthenticatedUser;
import com.dnastack.wes.wdl.ObjectWrapper;
import feign.FeignException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TransferService {

    private final ExternalAccountClient externalAccountClient;
    private final TransferServiceClient transferServiceClient;
    private final TransferConfig config;
    private final TaskScheduler taskScheduler;
    private final BlockingQueue<TransferContext> monitorQueue;


    TransferService(ExternalAccountClient externalAccountClient, TransferServiceClient transferServiceClient, TaskScheduler taskScheduler, AppConfig config) {
        this.externalAccountClient = externalAccountClient;
        this.transferServiceClient = transferServiceClient;
        this.taskScheduler = taskScheduler;
        this.config = config.getTransferConfig();
        this.monitorQueue = new LinkedBlockingQueue<>();
    }

    @PostConstruct
    public void init() {
        if (config.isEnabled()) {
            for (int i = 0; i < config.getNumMonitoringThreads(); i++) {
                taskScheduler.scheduleWithFixedDelay(this::monitorTransfer, 1000L);
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
        List<ObjectWrapper> objectsToTransfer = context.fileProcessor.getMappedObjects().stream()
            .filter(ObjectWrapper::getRequiresTransfer).collect(Collectors.toList());

        for (ObjectWrapper wrapper : objectsToTransfer) {
            String externalAccountId = wrapper.getTransferExternalAccount().getExternalAccountId();
            TransferRequest request = transferRequests
                .computeIfAbsent(externalAccountId, (k) -> new TransferRequest(externalAccountId));
            List<String> copyPair = Arrays.asList(wrapper.getSourceDestination(), wrapper.getTransferDestination());
            request.getCopyPairs().add(copyPair);
        }

        List<TransferJob> transfersToMonitor = new ArrayList<>();
        log.info("Submitting {} transfer jobs for run {}", transfersToMonitor.size(), context.getRunId());
        for (TransferRequest request : transferRequests.values()) {
            TransferJob job = transferServiceClient.submitTransferRequest(request, AuthenticatedUser.getSubject());
            transfersToMonitor.add(job);
        }

        context.setTransferJobs(transfersToMonitor);
        log.trace("Adding transfer jobs to monitoring queue");
        monitorQueue.add(context);
    }


    private List<ExternalAccount> getExternalAccountsForUser() {
        if (config.isEnabled()) {
            String userId = AuthenticatedUser.getBearerToken();
            log.trace("Retrieving external accounts for user {}", userId);
            List<ExternalAccount> externalAccounts = externalAccountClient
                .listExternalAccounts(userId);
            if (externalAccounts != null) {
                return externalAccounts;
            } else {
                return Collections.emptyList();
            }
        } else {
            return Collections.emptyList();
        }
    }


    public void configureObjectsForTransfer(List<ObjectWrapper> objectWrappers) {
        if (config.isEnabled() && objectWrappers != null && !objectWrappers.isEmpty()) {
            log.trace("Configuring object transfers for {} objects", objectWrappers.size());
            String stagingDirectoryPrefix = RandomStringUtils.randomAlphanumeric(6);
            List<ExternalAccount> externalAccounts = getExternalAccountsForUser();
            for (ObjectWrapper wrapper : objectWrappers) {
                configureObjectForTransfer(stagingDirectoryPrefix, externalAccounts, wrapper);
            }
        }
    }

    private void configureObjectForTransfer(String stagingDirectoryPrefix, List<ExternalAccount> externalAccounts, ObjectWrapper objectWrapper) {
        if (config.isEnabled()) {
            String mappedValue = objectWrapper.getMappedValue();
            URI mappedUri = URI.create(mappedValue);
            if (config.getObjectPrefixWhitelist().stream().anyMatch(mappedValue::startsWith)) {
                return;
            }

            Optional<ExternalAccount> optExternalAccount = externalAccounts.stream()
                .filter(account -> doesExternalAccountMatch(mappedUri, account)).findFirst();

            if (optExternalAccount.isPresent()) {
                ExternalAccount externalAccount = optExternalAccount.get();
                log.trace("Object {} requires transfer and an external account with id has been identified", mappedValue, externalAccount
                    .getExternalAccountId());
                String destinationLocation = generateTransferDestination(stagingDirectoryPrefix, mappedUri);
                objectWrapper.setTransferDestination(destinationLocation);
                objectWrapper.setSourceDestination(mappedValue);
                objectWrapper.setMappedValue(destinationLocation);
                objectWrapper.setRequiresTransfer(true);
                objectWrapper.setTransferExternalAccount(externalAccount);
            }
        }
    }

    private boolean doesExternalAccountMatch(URI mappedUri, ExternalAccount externalAccount) {
        //In this scenario the file is considered a local file
        if (mappedUri.getHost() == null && mappedUri.getScheme() == null) {
            return false;
        }

        Map<String, String> info = externalAccount.getInfo();
        String prefixString = null;
        if (info.containsKey("bucket")) {
            prefixString = info.get("bucket");
        } else if (info.containsKey("hostname")) {
            prefixString = info.get("hostname");
        }

        URI prefixUri = URI.create(prefixString);
        return mappedUri.getScheme().equals(prefixUri.getScheme()) && mappedUri.getHost().equals(prefixUri.getHost());

    }

    private String generateTransferDestination(String prefix, URI originalUri) {
        String stagingDirectory = config.getStagingDirectory();
        String path = originalUri.getPath();

        if (isCloudBucket(originalUri.getScheme())) {
            prefix = prefix + "/" + originalUri.getHost();
        }

        return stagingDirectory + "/" + prefix + "/" + path;
    }

    private boolean isCloudBucket(String scheme) {
        return scheme.equalsIgnoreCase("gs") || scheme.equalsIgnoreCase("s3");
    }

}
