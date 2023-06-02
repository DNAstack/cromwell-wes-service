package com.dnastack.wes.workflow;


import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
@ConditionalOnExpression("!${wes.workflows.authorizers.checksum.enabled:false} && !${wes.workflows.authorizers.url-allow-list.enabled:false}")
public class AllowAllWorkflowAuthorizer implements WorkflowAuthorizer {

    @Override
    public boolean authorize(String url, MultipartFile[] contents) {
        return true;
    }

}
