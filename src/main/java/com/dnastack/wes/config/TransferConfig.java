package com.dnastack.wes.config;


import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties("wes.transfer")
public class TransferConfig {

    /**
     * Enable the object transfer api. When enabled, workflow inputs which require transferring will be sent to the
     * configured transfer api and will localize the restricted access files to the target staging directory. When
     * disabled all transferring will be skipped and the workflow inputs will be used as-is
     */
    private boolean enabled = false;

    /**
     * The target directory to tansfer objects for staging to. If WES is deployed in a cloud environment, the staging
     * directory should correspond to the bucket or container of the underlying object storage. The workflow executor
     * and the transfer object should be able to access this directory
     */
    private String stagingDirectory = null;

    /**
     * A list of prefixes which the workflow executor knows how to access directly and therefore can be skipped from
     * transferring
     */
    private List<String> objectPrefixWhitelist = new ArrayList<>();

    /**
     * The number of threads to use for monitoring the status of submitted transfers. Transfers are performed
     * asynchronously
     */
    private Integer numMonitoringThreads = 1;

    /**
     * base URI fo the object transfer service
     */
    private String objectTransferUri = null;

}
