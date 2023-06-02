package com.dnastack.wes.storage;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AzureBlobStorageClientConfig {

    private String connectionString;
    private String container;
    private String stagingPath;
    private Long signedUrlTtl = 1000L * 60L * 60L * 24L;

}
