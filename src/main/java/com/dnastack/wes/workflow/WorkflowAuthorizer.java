package com.dnastack.wes.workflow;

import org.springframework.web.multipart.MultipartFile;

@FunctionalInterface
public interface WorkflowAuthorizer {
    boolean authorize(String url, MultipartFile[] contents);

}
