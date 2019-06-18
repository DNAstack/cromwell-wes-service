package com.dnastack.wes.service;

import com.dnastack.wes.wdl.WdlFileProcessor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class TransferService {


    @Async
    public void transferFilesAsync(WdlFileProcessor fileProcessor, Runnable callback) {

    }

}
