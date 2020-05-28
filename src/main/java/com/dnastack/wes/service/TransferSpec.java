package com.dnastack.wes.service;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TransferSpec {
    @Value
    public static class BlobSpec {
        private String accessKeyId;
        private String accessToken;
        private String sessionToken;
        private String uri;
    }

    private BlobSpec sourceSpec;
    private BlobSpec targetSpec;
}
