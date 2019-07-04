package com.dnastack.wes.controller;


import com.dnastack.wes.AppConfig;
import com.dnastack.wes.model.wes.RunId;
import com.dnastack.wes.model.wes.RunListResponse;
import com.dnastack.wes.model.wes.RunLog;
import com.dnastack.wes.model.wes.RunRequest;
import com.dnastack.wes.model.wes.RunStatus;
import com.dnastack.wes.model.wes.ServiceInfo;
import com.dnastack.wes.service.CromwellService;
import java.security.Principal;
import java.util.Map;
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

    @Autowired
    WesV1Controller(CromwellService adapter, AppConfig config) {
        this.adapter = adapter;
        this.config = config;
    }

    @PreAuthorize("permitAll()")
    @GetMapping(value = "/service-info", produces = {MediaType.APPLICATION_JSON_VALUE,
        MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ServiceInfo getServiceInfo(Principal user) {
        ServiceInfo serviceInfo = config.getServiceInfo();
        if (user != null) {
            serviceInfo.setSystemStateCounts(adapter.getSystemStateCounts());
        }
        serviceInfo.setWorkflowEngineVersions(adapter.getEngineVersions());
        return serviceInfo;
    }


    @PreAuthorize("isFullyAuthenticated()")
    @PostMapping(value = "/runs", produces = {MediaType.APPLICATION_JSON_VALUE,
        MediaType.APPLICATION_JSON_UTF8_VALUE}, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public RunId submitRun(Principal principal, @RequestPart("workflow_url") String workflowUrl,
        @RequestPart(name = "workflow_type", required = false) String workflowType,
        @RequestPart(name = "workflow_type_version", required = false) String workflowTypeVersion,
        @RequestPart(name = "workflow_engine_parameters", required = false) Map<String, String> workflowEngineParams,
        @RequestPart(name = "workflow_params", required = false) Map<String, Object> workflowParams,
        @RequestPart(name = "workflow_attachment", required = false) MultipartFile[] workflowAttachments,
        @RequestPart(name = "tags", required = false) Map<String, String> tags) {

        RunRequest runRequest = RunRequest.builder().workflowUrl(workflowUrl).workflowType(workflowType)
            .workflowEngineParameters(workflowEngineParams).workflowParams(workflowParams)
            .workflowTypeVersion(workflowTypeVersion).workflowAttachments(workflowAttachments).tags(tags).build();

        return adapter.execute(principal, runRequest);
    }


    @PreAuthorize("isFullyAuthenticated()")
    @GetMapping(path = "/runs", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public RunListResponse getRuns(@RequestParam(value = "page_size", required = false) Integer pageSize,
        @RequestParam(value = "page_token", required = false) String pageToken) {
        return adapter.listRuns(pageSize, pageToken);
    }


    @PreAuthorize("isFullyAuthenticated()")
    @GetMapping(value = "/runs/{run_id}", produces = {MediaType.APPLICATION_JSON_VALUE,
        MediaType.APPLICATION_JSON_UTF8_VALUE})
    public RunLog getRun(@PathVariable("run_id") String runId) {
        return adapter.getRun(runId);
    }


    @PreAuthorize("isFullyAuthenticated()")
    @GetMapping(value = "/runs/{run_id}/status", produces = {MediaType.APPLICATION_JSON_VALUE,
        MediaType.APPLICATION_JSON_UTF8_VALUE})
    public RunStatus getRunStatus(@PathVariable("run_id") String runId) {
        return adapter.getRunStatus(runId);
    }


    @PreAuthorize("isFullyAuthenticated()")
    @PostMapping(path = "/runs/{runId}/cancel", produces =
        MediaType.APPLICATION_JSON_UTF8_VALUE)
    public RunId cancelRun(@PathVariable("runId") String runId) {
        return adapter.cancel(runId);
    }

}
