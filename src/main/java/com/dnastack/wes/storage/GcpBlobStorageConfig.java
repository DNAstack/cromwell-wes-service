package com.dnastack.wes.storage;

import lombok.Data;

import java.net.URI;
import java.time.Duration;

@Data
public class GcpBlobStorageConfig {

    private URI stagingLocation;
    private String serviceAccountJson = null;
    private String project;
    private String billingProject;
    private Duration signdUrlTtl = Duration.ofDays(1);

}
