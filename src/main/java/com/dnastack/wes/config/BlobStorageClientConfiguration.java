package com.dnastack.wes.config;

import com.dnastack.wes.config.BlobStorageClientConfig.ClientName;
import com.dnastack.wes.storage.client.BlobStorageClient;
import com.dnastack.wes.storage.client.gcp.GcpBlobStorageClient;
import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BlobStorageClientConfiguration {

    @Bean
    public BlobStorageClient blobStorageClient(BlobStorageClientConfig config) throws IOException {
        if (config.getName().equals(ClientName.GCP)) {
            return new GcpBlobStorageClient(config.getGcp());
        } else {
            throw new RuntimeException("Could not configure blob storage client, please specify at least one configuration");
        }
    }

}
