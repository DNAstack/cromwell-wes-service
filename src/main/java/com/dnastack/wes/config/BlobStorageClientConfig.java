package com.dnastack.wes.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties("wes.blob-storage-client")
public class BlobStorageClientConfig {


    private ClientName name;

    private GcpBlobStorageConfig gcp = new GcpBlobStorageConfig();


    public enum ClientName  {
        GCP
    }

}