package com.dnastack.wes.config;

import java.net.URI;
import java.time.Duration;
import lombok.Data;

@Data
public class GcpBlobStorageConfig {

    private URI stagingLocation;
    private String serviceAccountJson = null;
    private String project;
    private Duration signdUrlTtl = Duration.ofDays(1);
}
