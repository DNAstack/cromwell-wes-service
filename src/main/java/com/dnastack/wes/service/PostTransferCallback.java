package com.dnastack.wes.service;

@FunctionalInterface
public interface PostTransferCallback {

    void callAfterTransfer(Throwable throwable, String runId);

}
