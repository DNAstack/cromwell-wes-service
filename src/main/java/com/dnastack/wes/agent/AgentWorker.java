package com.dnastack.wes.agent;

import com.dnastack.oauth.client.OAuthClientConfiguration;
import com.dnastack.oauth.client.OAuthClientFactory;
import com.dnastack.wes.api.RunId;
import com.dnastack.wes.api.RunLog;
import com.dnastack.wes.api.RunRequest;
import com.dnastack.wes.api.State;
import com.dnastack.wes.cromwell.CromwellClientConfiguration;
import com.dnastack.wes.cromwell.CromwellHealthIndicator;
import com.dnastack.wes.cromwell.CromwellService;
import com.dnastack.wes.utils.MultipartUtils;
import feign.Logger;
import feign.okhttp.OkHttpClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
public class AgentWorker {


    private final CromwellHealthIndicator cromwellHealthIndicator;
    private final CromwellService adapter;
    private final WorkbenchClient workbenchClient;

    public AgentWorker(CromwellHealthIndicator cromwellHealthIndicator, CromwellService adapter) {
        this.cromwellHealthIndicator = cromwellHealthIndicator;
        this.adapter = adapter;
        OAuthClientConfiguration configuration = OAuthClientConfiguration.builder()
            .clientId("workbench-agent-1")
            .clientSecret("workbench-agent-1")
            .resource("http://localhost:9095/")
            .tokenUri("http://localhost:8081/oauth/token")
            .build();
        this.workbenchClient = new OAuthClientFactory(configuration, new OkHttpClient()).builderWithCommonSetup(configuration)
            .logger(new CromwellClientConfiguration.SimpleLogger()).logLevel(Logger.Level.BASIC)
            .target(WorkbenchClient.class, "http://localhost:9095");
    }


    @Scheduled(fixedRate = 1000)
    public void doWork() {
        AgentWorkList agentWorkList = workbenchClient.getAgentWork();
        for (AgentWork work : agentWorkList.getWork()) {
            switch (work) {
                case SubmitWorkflowAgentWork submitWorkflowAgentWork:
                    submitWorkflowAndReport(submitWorkflowAgentWork);
                    break;
                case UpdateWorkflowStatusAgentWork updateWorkflowStatusAgentWork:
                    updateWorkflowStatusAndReport(updateWorkflowStatusAgentWork);
                    break;
                case HealthCheckAgentWork healthCheckAgentWork:
                    doHealthCheckAndReport(healthCheckAgentWork);
                    break;
                case CancelWorkflowAgentWork cancelWorkflowAgentWork:
                    cancelWorkflow(cancelWorkflowAgentWork);
                    break;

                default:
                    throw new IllegalStateException("Unexpected value: " + work);
            }

        }
    }

    private void cancelWorkflow(CancelWorkflowAgentWork cancelWorkflowAgentWork) {
        adapter.cancel(cancelWorkflowAgentWork.getRunId());
    }

    private void doHealthCheckAndReport(HealthCheckAgentWork healthCheckAgentWork) {
        Set<Check> checks = Set.of(
            Check.builder().type(Check.CheckType.CONNECTIVITY).outcome(cromwellHealthIndicator.check()).build()
        );

        List<HealthCheckAgentReport> reports = List.of(HealthCheckAgentReport.builder()
        .workId(healthCheckAgentWork.getWorkId())
        .reportId(UUID.randomUUID().toString())
        .checks(checks)
        .build());

        workbenchClient.report(new AgentReportList(reports));
    }

    private void updateWorkflowStatusAndReport(UpdateWorkflowStatusAgentWork updateWorkflowStatusAgentWork) {
        RunLog run = adapter.getRun(updateWorkflowStatusAgentWork.getRunId());
        List<UpdateWorkflowStatusAgentReport> reports = List.of(UpdateWorkflowStatusAgentReport.builder()
            .workId(updateWorkflowStatusAgentWork.getWorkId())
            .reportId(UUID.randomUUID().toString())
            .runLog(run)
            .build());
        workbenchClient.report(new AgentReportList(reports));
    }

    private void submitWorkflowAndReport(SubmitWorkflowAgentWork submitWorkflowAgentWork) {
        UpdateWorkflowStatusAgentReport updateWorkflowStatusAgentReport = UpdateWorkflowStatusAgentReport.builder()
            .workId(submitWorkflowAgentWork.getWorkId())
            .reportId(UUID.randomUUID().toString()).build();
        RunLog runLog = RunLog.builder().build();
        try {
            RunId runId = adapter.execute(
                RunRequest.builder()
                    .workflowUrl(submitWorkflowAgentWork.getWorkflowUrl())
                    .workflowType(submitWorkflowAgentWork.getWorkflowType())
                    .workflowParams(submitWorkflowAgentWork.getWorkflowParams())
                    .workflowEngineParameters(submitWorkflowAgentWork.getWorkflowEngineParameters())
                    .workflowAttachments(MultipartUtils.getAsMultipart(submitWorkflowAgentWork.getWorkflowFiles()))
                    .workflowTypeVersion(submitWorkflowAgentWork.getWorkflowTypeVersion())
                    .build());
            runLog.setRunId(runId.getRunId());
        } catch (Exception e) {
            runLog.setState(State.EXECUTOR_ERROR);
        }
        updateWorkflowStatusAgentReport.setRunLog(runLog);
        workbenchClient.report(new AgentReportList(List.of(updateWorkflowStatusAgentReport)));
    }

}
