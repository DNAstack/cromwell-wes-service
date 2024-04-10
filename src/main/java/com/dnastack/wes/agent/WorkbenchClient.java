package com.dnastack.wes.agent;

import feign.Headers;
import feign.RequestLine;

public interface WorkbenchClient {


    @RequestLine("GET /agent/get-work")
    @Headers("Accept: application/json")
    AgentWorkList getAgentWork();


    @RequestLine("POST /agent/report")
    @Headers("Content-Type: application/json")
    void report(AgentReportList reportList);

}
