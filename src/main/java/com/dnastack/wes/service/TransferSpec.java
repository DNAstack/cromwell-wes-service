package com.dnastack.wes.service;

import lombok.Value;

@Value
public class TransferSpec {
    @Value
    public static class BlobSpec {
        private String accessToken;
        private String uri;
    }

    private BlobSpec sourceSpec;
    private BlobSpec targetSpec;
}
