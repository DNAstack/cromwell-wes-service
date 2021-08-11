package com.dnastack.wes.storage;

import lombok.Data;

@Data
public class AzureBlobStorageClientConfig {

    public String connectionString;
    public String container;
    public String stagingPath;
    public Long signedUrlTtl = 1000L * 60L * 60L * 24L ;
}
