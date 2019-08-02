package com.dnastack.wes.service;

import com.dnastack.wes.model.transfer.TransferJob;
import com.dnastack.wes.wdl.WdlFileProcessor;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TransferContext {

    String runId;
    WdlFileProcessor fileProcessor;
    List<TransferJob> transferJobs;
    PostTransferCallback callback;


    public void complete() {
        callback.callAfterTransfer(null, runId);
    }

    public void fail(Throwable e) {
        callback.callAfterTransfer(e, runId);
    }

}
