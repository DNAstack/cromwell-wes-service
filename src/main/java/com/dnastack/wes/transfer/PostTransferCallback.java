package com.dnastack.wes.transfer;

@FunctionalInterface
public interface PostTransferCallback {

    void callAfterTransfer(Throwable throwable, String runId);

}
