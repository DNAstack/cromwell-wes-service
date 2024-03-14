package com.dnastack.wes.files;

import com.dnastack.audit.aspect.AuditActionUri;
import com.dnastack.wes.utils.RangeHeaderUtils;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/ga4gh/wes/v1")
public class RunFileController {


    private final RunFileService runFileService;

    public RunFileController(RunFileService runFileService) {this.runFileService = runFileService;}

    @AuditActionUri("wes:run:files:list")
    @PreAuthorize("@accessEvaluator.canAccessResource('/ga4gh/wes/v1/runs/' + #runId, 'wes:runs:read', 'wes')")
    @GetMapping(value = "/runs/{runId}/files", produces = { MediaType.APPLICATION_JSON_VALUE })
    public RunFiles getRunFiles(@PathVariable String runId) {
        return runFileService.getRunFiles(runId);
    }

    @AuditActionUri("wes:run:files:get-metadata")
    @PreAuthorize("@accessEvaluator.canAccessResource('/ga4gh/wes/v1/runs/' + #runId, 'wes:runs:read', 'wes')")
    @GetMapping(value = "/runs/{runId}/file", produces = { MediaType.APPLICATION_JSON_VALUE })
    public RunFile getRunFile(@PathVariable String runId, @RequestParam String path) {
        return runFileService.getRunFile(runId,path);
    }

    @AuditActionUri("wes:run:files:get-metadata")
    @PreAuthorize("@accessEvaluator.canAccessResource('/ga4gh/wes/v1/runs/' + #runId, 'wes:runs:read', 'wes')")
    @GetMapping(value = "/runs/{runId}/file", produces = { MediaType.APPLICATION_OCTET_STREAM_VALUE })
    public void streamFileContents(
        HttpServletResponse response,
        @RequestHeader HttpHeaders headers,
        @PathVariable String runId,
        @RequestParam String path
    ) throws IOException {
        runFileService.streamRunFile(response, runId, path, RangeHeaderUtils.getRangeFromHeaders(response,headers));
    }



    @AuditActionUri("wes:run:files:delete")
    @PreAuthorize("@accessEvaluator.canAccessResource('/ga4gh/wes/v1/runs/' + #runId, 'wes:runs:write', 'wes')")
    @DeleteMapping(value = "/runs/{run_id}/files", produces = { MediaType.APPLICATION_JSON_VALUE })
    public RunFileDeletions deleteRunFiles(
        @PathVariable("run_id") String runId,
        @RequestParam(value = "async", required = false) boolean async
    ) {
        return runFileService.deleteRunFiles(runId, async);
    }

}
