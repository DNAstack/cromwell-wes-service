package com.dnastack.wes.transfer;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TransferSpec {

    private BlobSpec sourceSpec;
    private BlobSpec targetSpec;

    @Value
    public static class BlobSpec {

        private String accessKeyId;
        private String accessToken;
        private String sessionToken;
        private String uri;

    }

}
