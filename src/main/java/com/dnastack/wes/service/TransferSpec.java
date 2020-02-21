package com.dnastack.wes.service;

import lombok.Value;

@Value
public class TransferSpec {
    private String accessToken;
    private String sourceUri;
    private String targetUri;
}
