package com.dnastack.wes.service;

import com.dnastack.wes.model.transfer.TransferJob;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TransferContext {

    String runId;
    Map<String, String[]> objectsToTransfer;
    List<TransferJob> transferJobs;
    PostTransferCallback callback;


    public void complete() {
        callback.callAfterTransfer(null, runId);
    }

    public void fail(Throwable e) {
        callback.callAfterTransfer(e, runId);
    }

}
