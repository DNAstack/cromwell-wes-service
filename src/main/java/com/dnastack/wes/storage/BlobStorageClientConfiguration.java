package com.dnastack.wes.storage;

import com.dnastack.wes.storage.BlobStorageClientConfig.ClientName;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class BlobStorageClientConfiguration {

    @Bean
    public BlobStorageClient blobStorageClient(BlobStorageClientConfig config) throws IOException {
        if (config.getName().equals(ClientName.GCP)) {
            return new GcpBlobStorageClient(config.getGcp());
        } else if (config.getName().equals(ClientName.ABS)) {
            return new AzureBlobStorageClient(config.getAbs());
        } else if (config.getName().equals(ClientName.LOCAL)) {
            return new LocalBlobStorageClient(config.getLocal());
        } else {
            throw new StorageException("Could not configure blob storage client, please specify at least one configuration");
        }
    }

}
