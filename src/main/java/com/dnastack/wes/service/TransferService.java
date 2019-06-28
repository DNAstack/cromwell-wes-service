package com.dnastack.wes.service;

import com.dnastack.wes.wdl.WdlFileProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TransferService {


    @Async
    public void transferFilesAsync(WdlFileProcessor fileProcessor, PostTransferCallback callback) {
        try {
            Throwable throwable = null;
            try {
            } catch (Exception e) {
                throwable = e;
            }
            callback.callAfterTransfer(throwable, fileProcessor.getMappedObjects());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

}
