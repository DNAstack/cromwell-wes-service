package com.dnastack.wes.cromwell;

import feign.Headers;
import feign.Param;
import feign.QueryMap;
import feign.RequestLine;
import org.springframework.http.MediaType;

import java.util.Map;


public interface CromwellClient {

    String API_VERSION = "v1";


    @RequestLine("GET /engine/" + API_VERSION + "/version")
    CromwellVersion getVersion();

    @RequestLine("GET /engine/" + API_VERSION + "/status")
    Map<String,Object> getStatus();

    @RequestLine("GET /api/workflows/" + API_VERSION + "/query")
    CromwellResponse listWorkflows();


    @RequestLine("GET /api/workflows/" + API_VERSION + "/query")
    @Headers("Content-Type: " + MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    CromwellResponse listWorkflows(@QueryMap CromwellSearch search);


    @RequestLine("GET /api/workflows/" + API_VERSION + "/{id}/status")
    CromwellStatus getStatus(@Param("id") String id);


    @RequestLine("GET /api/workflows/" + API_VERSION + "/{id}/metadata?expandSubWorkflows=true")
    CromwellMetadataResponse getMetadata(@Param("id") String id);


    @RequestLine("POST /api/workflows/" + API_VERSION + "/{id}/abort")
    CromwellStatus abortWorkflow(@Param("id") String id);


    @RequestLine("POST /api/workflows/" + API_VERSION)
    @Headers("Content-Type: " + MediaType.MULTIPART_FORM_DATA_VALUE)
    CromwellStatus createWorkflow(CromwellExecutionRequest executionRequest);

}
