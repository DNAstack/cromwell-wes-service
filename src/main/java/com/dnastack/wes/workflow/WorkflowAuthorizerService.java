package com.dnastack.wes.workflow;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class WorkflowAuthorizerService {

    private final List<WorkflowAuthorizer> authorizers;

    public WorkflowAuthorizerService(List<WorkflowAuthorizer> authorizers) {this.authorizers = authorizers;}

    public void authorize(String url, MultipartFile[] attachments) {

        if (authorizers.stream().noneMatch(authorizer -> authorizer.authorize(url, attachments))) {
            throw new UnauthorizedWorkflowException("Workflow is not authorized");
        }

    }

}
