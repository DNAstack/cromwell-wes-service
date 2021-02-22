package com.dnastack.wes.controller;


import com.dnastack.wes.config.AppConfig;
import com.dnastack.wes.config.TransferConfig;
import com.dnastack.wes.model.wes.RunId;
import com.dnastack.wes.model.wes.RunListResponse;
import com.dnastack.wes.model.wes.RunLog;
import com.dnastack.wes.model.wes.RunRequest;
import com.dnastack.wes.model.wes.RunStatus;
import com.dnastack.wes.model.wes.ServiceInfo;
import com.dnastack.wes.security.AccessEvaluator;
import com.dnastack.wes.security.AuthenticatedUser;
import com.dnastack.wes.service.CromwellService;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;


@Slf4j
@RestController
@RequestMapping("/ga4gh/wes/v1")
public class WesV1Controller {


    private CromwellService adapter;
    private AppConfig config;
    private TransferConfig transferConfig;
    private AccessEvaluator accessEvaluator;

    @Autowired
    WesV1Controller(CromwellService adapter, AppConfig config, TransferConfig transferConfig, AccessEvaluator accessEvaluator) {
        this.adapter = adapter;
        this.config = config;
        this.transferConfig = transferConfig;
        this.accessEvaluator = accessEvaluator;
    }

    @PreAuthorize("permitAll()")
    @GetMapping(value = "/service-info", produces = {MediaType.APPLICATION_JSON_VALUE})
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

        tags.put("tranfser-service-enabled", String.valueOf(transferConfig.isEnabled()));
        serviceInfo.setWorkflowEngineVersions(adapter.getEngineVersions());
        return serviceInfo;
    }

    @PreAuthorize("@accessEvaluator.canAccessResource('/ga4gh/wes/v1/runs', 'wes:execute', 'wes')")
    @PostMapping(value = "/runs", produces = {
        MediaType.APPLICATION_JSON_VALUE}, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public RunId submitRun(@RequestPart("workflow_url") String workflowUrl,
        @RequestPart(name = "workflow_type", required = false) String workflowType,
        @RequestPart(name = "workflow_type_version", required = false) String workflowTypeVersion,
        @RequestPart(name = "workflow_engine_parameters", required = false) Map<String, String> workflowEngineParams,
        @RequestPart(name = "workflow_params", required = false) Map<String, Object> workflowParams,
        @RequestPart(name = "tags", required = false) Map<String, String> tags,
        @RequestPart(name = "workflow_attachment", required = false) MultipartFile[] workflowAttachments) throws IOException {

        RunRequest runRequest = RunRequest.builder().workflowUrl(workflowUrl).workflowType(workflowType)
            .workflowEngineParameters(workflowEngineParams)
            .workflowParams(workflowParams)
            .workflowTypeVersion(workflowTypeVersion).workflowAttachments(workflowAttachments)
            .tags(tags).build();

        return adapter.execute(AuthenticatedUser.getSubject(), runRequest);
    }

    @PreAuthorize("@accessEvaluator.canAccessResource('/ga4gh/wes/v1/runs', 'wes:runs:read', 'wes')")
    @GetMapping(path = "/runs", produces = MediaType.APPLICATION_JSON_VALUE)
    public RunListResponse getRuns(@RequestParam(value = "page_size", required = false) Integer pageSize,
        @RequestParam(value = "page_token", required = false) String pageToken) {
        return adapter.listRuns(pageSize, pageToken);
    }

    @PreAuthorize("@accessEvaluator.canAccessResource('/ga4gh/wes/v1/runs/'+#runId, 'wes:runs:read', 'wes')")
    @GetMapping(value = "/runs/{run_id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public RunLog getRun(HttpServletRequest request, @PathVariable("run_id") String runId) {
        return adapter.getRun(runId);
    }

    @PreAuthorize("@accessEvaluator.canAccessResource('/ga4gh/wes/v1/runs/' + #runId , 'wes:runs:read', 'wes')")
    @GetMapping(value = "/runs/{run_id}/status", produces = {MediaType.APPLICATION_JSON_VALUE})
    public RunStatus getRunStatus(@PathVariable("run_id") String runId) {
        return adapter.getRunStatus(runId);
    }

    @PreAuthorize("@accessEvaluator.canAccessResource('/ga4gh/wes/v1/runs/' + #runId, 'wes:runs:cancel', 'wes')")
    @PostMapping(path = "/runs/{runId}/cancel", produces = MediaType.APPLICATION_JSON_VALUE)
    public RunId cancelRun(@PathVariable("runId") String runId) {
        return adapter.cancel(runId);
    }

    @PreAuthorize("@accessEvaluator.canAccessResource('/ga4gh/wes/v1/runs/' + #runId, 'wes:runs:read', 'wes')")
    @GetMapping(value = "/runs/{runId}/logs/stderr", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void getStderr(HttpServletResponse response, @PathVariable String runId) throws IOException {
        adapter.getLogBytes(response.getOutputStream(), runId);
    }

    @PreAuthorize("@accessEvaluator.canAccessResource('/ga4gh/wes/v1/runs/' + #runId, 'wes:runs:read', 'wes')")
    @GetMapping(value = "/runs/{runId}/logs/task/{taskName}/{index}/stderr", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void getStderr(HttpServletResponse response, @PathVariable String runId, @PathVariable String taskName, @PathVariable int index) throws IOException {
        adapter.getLogBytes(response.getOutputStream(), runId, taskName, index, "stderr");
    }

    @PreAuthorize("@accessEvaluator.canAccessResource('/ga4gh/wes/v1/runs/' + #runId, 'wes:runs:read', 'wes')")
    @GetMapping(value = "/runs/{runId}/logs/task/{taskName}/{index}/stdout", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void getStdout(HttpServletResponse response, @PathVariable String runId, @PathVariable String taskName, @PathVariable int index) throws IOException {
        adapter.getLogBytes(response.getOutputStream(), runId, taskName, index, "stdout");
    }


}
