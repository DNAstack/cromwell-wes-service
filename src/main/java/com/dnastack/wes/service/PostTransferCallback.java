package com.dnastack.wes.service;

import com.dnastack.wes.wdl.ObjectWrapper;
import java.util.List;

@FunctionalInterface
public interface PostTransferCallback {

    void callAfterTransfer(Throwable throwable, List<ObjectWrapper> fileProcessor);

}
