package com.dnastack.wes.transfer;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class TransferContext {

    String runId;
    List<TransferSpec> objectsToTransfer;
    List<TransferJob> transferJobs;
    PostTransferCallback callback;


    public void complete() {
        callback.callAfterTransfer(null, runId);
    }

    public void fail(Throwable e) {
        callback.callAfterTransfer(e, runId);
    }

}
