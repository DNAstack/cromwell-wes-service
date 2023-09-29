package com.dnastack.wes.api;


import com.dnastack.audit.aspect.AuditActionUri;
import com.dnastack.audit.aspect.AuditIgnore;
import com.dnastack.wes.AppConfig;
import com.dnastack.wes.cromwell.CromwellService;
import com.dnastack.wes.security.AuthenticatedUser;
import com.dnastack.wes.workflow.WorkflowAuthorizerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Slf4j
@RestController
@RequestMapping("/ga4gh/wes/v1")
public class WesV1Controller {


    private final WorkflowAuthorizerService workflowAuthorizerService;
    private final CromwellService adapter;
    private final AppConfig config;

    private final boolean securityEnabled;

    @Autowired
    WesV1Controller(
        WorkflowAuthorizerService workflowAuthorizerService,
        CromwellService adapter,
        AppConfig config,
        @Value("${security.authentication.enabled}") boolean securityEnabled
    ) {
        this.workflowAuthorizerService = workflowAuthorizerService;
        this.adapter = adapter;
        this.config = config;
        this.securityEnabled = securityEnabled;
    }

    @AuditActionUri("wes:service-info")
    @PreAuthorize("permitAll()")
    @GetMapping(value = "/service-info", produces = { MediaType.APPLICATION_JSON_VALUE })
    public ServiceInfo getServiceInfo() {
        ServiceInfo serviceInfo = config.getServiceInfo();
        if (!securityEnabled || (AuthenticatedUser.getSubject() != null)) {
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


    @PreAuthorize("@accessEvaluator.canAccessResource('/ga4gh/wes/v1/runs', 'wes:execute', 'wes')")
    @PostMapping(value = "/runs", produces = {
        MediaType.APPLICATION_JSON_VALUE
    }, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public RunId submitRun(
        @RequestPart("workflow_url") String workflowUrl,
        @RequestPart(name = "workflow_type", required = false) String workflowType,
        @RequestPart(name = "workflow_type_version", required = false) String workflowTypeVersion,
        @AuditIgnore @RequestPart(name = "workflow_engine_parameters", required = false) Map<String, String> workflowEngineParams,
        @AuditIgnore @RequestPart(name = "workflow_params", required = false) Map<String, Object> workflowParams,
        @RequestPart(name = "tags", required = false) Map<String, String> tags,
        @AuditIgnore @RequestPart(name = "workflow_attachment", required = false) MultipartFile[] workflowAttachments
    ) {

        RunRequest runRequest = RunRequest.builder().workflowUrl(workflowUrl).workflowType(workflowType)
            .workflowEngineParameters(workflowEngineParams)
            .workflowParams(workflowParams)
            .workflowTypeVersion(workflowTypeVersion).workflowAttachments(workflowAttachments)
            .tags(tags).build();

        workflowAuthorizerService.authorize(workflowUrl, workflowAttachments);
        return adapter.execute(runRequest);
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
    public RunLog getRun(@PathVariable("run_id") String runId) {
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
    public void getStderr(HttpServletResponse response, @RequestHeader HttpHeaders headers, @PathVariable String runId) throws IOException {
        adapter.getLogBytes(response.getOutputStream(), runId,getRangeFromHeaders(headers));
    }

    @AuditActionUri("wes:run:stderr")
    @PreAuthorize("@accessEvaluator.canAccessResource('/ga4gh/wes/v1/runs/' + #runId, 'wes:runs:read', 'wes')")
    @GetMapping(value = "/runs/{runId}/logs/task/{taskId}/stderr", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void getTaskStderr(
        HttpServletResponse response,
        @RequestHeader HttpHeaders headers,
        @PathVariable String runId,
        @PathVariable String taskId
    ) throws IOException {
        adapter.getLogBytes(response.getOutputStream(), runId, taskId, "stderr",getRangeFromHeaders(headers));
    }

    @AuditActionUri("wes:run:stdout")
    @PreAuthorize("@accessEvaluator.canAccessResource('/ga4gh/wes/v1/runs/' + #runId, 'wes:runs:read', 'wes')")
    @GetMapping(value = "/runs/{runId}/logs/task/{taskId}/stdout", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void getTaskStdout(
        HttpServletResponse response,
        @RequestHeader HttpHeaders headers,
        @PathVariable String runId,
        @PathVariable String taskId
    ) throws IOException {
        adapter.getLogBytes(response.getOutputStream(), runId, taskId, "stdout",getRangeFromHeaders(headers));
    }

    @AuditActionUri("wes:run:stderr")
    @PreAuthorize("@accessEvaluator.canAccessResource('/ga4gh/wes/v1/runs/' + #runId, 'wes:runs:read', 'wes')")
    @GetMapping(value = "/runs/{runId}/logs/task/{taskName}/{index}/stderr", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void getTaskStderr(
        HttpServletResponse response,
        @RequestHeader HttpHeaders headers,
        @PathVariable String runId,
        @PathVariable String taskName,
        @PathVariable int index
    ) throws IOException {
        adapter.getLogBytes(response.getOutputStream(), runId, taskName, index, "stderr",getRangeFromHeaders(headers));
    }

    @AuditActionUri("wes:run:stdout")
    @PreAuthorize("@accessEvaluator.canAccessResource('/ga4gh/wes/v1/runs/' + #runId, 'wes:runs:read', 'wes')")
    @GetMapping(value = "/runs/{runId}/logs/task/{taskName}/{index}/stdout", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void getTaskStdout(
        HttpServletResponse response,
        @RequestHeader HttpHeaders headers,
        @PathVariable String runId,
        @PathVariable String taskName,
        @PathVariable int index
    ) throws IOException {
        adapter.getLogBytes(response.getOutputStream(), runId, taskName, index, "stdout", getRangeFromHeaders(headers));
    }

    private HttpRange getRangeFromHeaders(HttpHeaders headers){
        List<HttpRange> ranges = headers.getRange();
        if (ranges.isEmpty()){
            return null;
        } else {
            // only return the first range parsed
            return ranges.get(0);
        }
    }

}
