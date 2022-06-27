package com.dnastack.wes.api;


import com.dnastack.audit.aspect.AuditActionUri;
import com.dnastack.audit.aspect.AuditIgnore;
import com.dnastack.wes.AppConfig;
import com.dnastack.wes.cromwell.CromwellService;
import com.dnastack.wes.security.AccessEvaluator;
import com.dnastack.wes.security.AuthenticatedUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


@Slf4j
@RestController
@RequestMapping("/ga4gh/wes/v1")
public class WesV1Controller {


    private CromwellService adapter;
    private AppConfig config;
    private AccessEvaluator accessEvaluator;

    @Autowired
    WesV1Controller(CromwellService adapter, AppConfig config, AccessEvaluator accessEvaluator) {
        this.adapter = adapter;
        this.config = config;
        this.accessEvaluator = accessEvaluator;
    }

    @AuditActionUri("wes:service-info")
    @PreAuthorize("permitAll()")
    @GetMapping(value = "/service-info", produces = { MediaType.APPLICATION_JSON_VALUE })
    public ServiceInfo getServiceInfo() {
        ServiceInfo serviceInfo = config.getServiceInfo();
        if (AuthenticatedUser.getSubject() != null && accessEvaluator
            .canAccessResource("/ga4gh/wes/v1/service-info", Set.of("wes:runs:read"), Set.of("wes"))) {
            serviceInfo.setSystemStateCounts(adapter.getSystemStateCounts());
        }

        Map<String, String> tags = serviceInfo.getTags();
        if (tags == null) {
            tags = new HashMap<>();
        }
        serviceInfo.setTags(tags);
        serviceInfo.setWorkflowEngineVersions(adapter.getEngineVersions());
        return serviceInfo;
    }

    @AuditIgnore
    @PreAuthorize("@accessEvaluator.canAccessResource('/ga4gh/wes/v1/runs', 'wes:execute', 'wes')")
    @PostMapping(value = "/runs", produces = {
        MediaType.APPLICATION_JSON_VALUE
    }, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public RunId submitRun(
        @RequestPart("workflow_url") String workflowUrl,
        @RequestPart(name = "workflow_type", required = false) String workflowType,
        @RequestPart(name = "workflow_type_version", required = false) String workflowTypeVersion,
        @RequestPart(name = "workflow_engine_parameters", required = false) Map<String, String> workflowEngineParams,
        @RequestPart(name = "workflow_params", required = false) Map<String, Object> workflowParams,
        @RequestPart(name = "tags", required = false) Map<String, String> tags,
        @RequestPart(name = "workflow_attachment", required = false) MultipartFile[] workflowAttachments
    ) throws IOException {

        RunRequest runRequest = RunRequest.builder().workflowUrl(workflowUrl).workflowType(workflowType)
            .workflowEngineParameters(workflowEngineParams)
            .workflowParams(workflowParams)
            .workflowTypeVersion(workflowTypeVersion).workflowAttachments(workflowAttachments)
            .tags(tags).build();

        return adapter.execute(AuthenticatedUser.getSubject(), runRequest);
    }

    @AuditActionUri("wes:runs:list")
    @PreAuthorize("@accessEvaluator.canAccessResource('/ga4gh/wes/v1/runs', 'wes:runs:read', 'wes')")
    @GetMapping(path = "/runs", produces = MediaType.APPLICATION_JSON_VALUE)
    public RunListResponse getRuns(
        @RequestParam(value = "page_size", required = false) Integer pageSize,
        @RequestParam(value = "page_token", required = false) String pageToken
    ) {
        return adapter.listRuns(pageSize, pageToken);
    }

    @AuditActionUri("wes:run:read")
    @PreAuthorize("@accessEvaluator.canAccessResource('/ga4gh/wes/v1/runs/'+#runId, 'wes:runs:read', 'wes')")
    @GetMapping(value = "/runs/{run_id}", produces = { MediaType.APPLICATION_JSON_VALUE })
    public RunLog getRun(HttpServletRequest request, @PathVariable("run_id") String runId) {
        return adapter.getRun(runId);
    }

    @AuditActionUri("wes:run:status")
    @PreAuthorize("@accessEvaluator.canAccessResource('/ga4gh/wes/v1/runs/' + #runId , 'wes:runs:read', 'wes')")
    @GetMapping(value = "/runs/{run_id}/status", produces = { MediaType.APPLICATION_JSON_VALUE })
    public RunStatus getRunStatus(@PathVariable("run_id") String runId) {
        return adapter.getRunStatus(runId);
    }

    @AuditActionUri("wes:run:cancel")
    @PreAuthorize("@accessEvaluator.canAccessResource('/ga4gh/wes/v1/runs/' + #runId, 'wes:runs:cancel', 'wes')")
    @PostMapping(path = "/runs/{runId}/cancel", produces = MediaType.APPLICATION_JSON_VALUE)
    public RunId cancelRun(@PathVariable("runId") String runId) {
        return adapter.cancel(runId);
    }

    @AuditActionUri("wes:run:stderr")
    @PreAuthorize("@accessEvaluator.canAccessResource('/ga4gh/wes/v1/runs/' + #runId, 'wes:runs:read', 'wes')")
    @GetMapping(value = "/runs/{runId}/logs/stderr", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void getStderr(HttpServletResponse response, @PathVariable String runId) throws IOException {
        adapter.getLogBytes(response.getOutputStream(), runId);
    }

    @AuditActionUri("wes:run:stderr")
    @PreAuthorize("@accessEvaluator.canAccessResource('/ga4gh/wes/v1/runs/' + #runId, 'wes:runs:read', 'wes')")
    @GetMapping(value = "/runs/{runId}/logs/task/{taskName}/{index}/stderr", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void getStderr(HttpServletResponse response, @PathVariable String runId, @PathVariable String taskName, @PathVariable int index) throws IOException {
        adapter.getLogBytes(response.getOutputStream(), runId, taskName, index, "stderr");
    }

    @AuditActionUri("wes:run:stdout")
    @PreAuthorize("@accessEvaluator.canAccessResource('/ga4gh/wes/v1/runs/' + #runId, 'wes:runs:read', 'wes')")
    @GetMapping(value = "/runs/{runId}/logs/task/{taskName}/{index}/stdout", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void getStdout(HttpServletResponse response, @PathVariable String runId, @PathVariable String taskName, @PathVariable int index) throws IOException {
        adapter.getLogBytes(response.getOutputStream(), runId, taskName, index, "stdout");
    }


}
