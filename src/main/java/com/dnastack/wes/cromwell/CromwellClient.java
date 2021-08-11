package com.dnastack.wes.cromwell;

import com.dnastack.wes.cromwell.CromwellExecutionRequest;
import com.dnastack.wes.cromwell.CromwellMetadataResponse;
import com.dnastack.wes.cromwell.CromwellResponse;
import com.dnastack.wes.cromwell.CromwellSearch;
import com.dnastack.wes.cromwell.CromwellStatus;
import com.dnastack.wes.cromwell.CromwellVersion;
import feign.Headers;
import feign.Param;
import feign.QueryMap;
import feign.RequestLine;
import org.springframework.http.MediaType;


public interface CromwellClient {

    String API_VERSION = "v1";


    @RequestLine("GET /engine/" + API_VERSION + "/version")
    CromwellVersion getVersion();

    @RequestLine("GET /api/workflows/" + API_VERSION + "/query")
    CromwellResponse listWorkflows();


    @RequestLine("GET /api/workflows/" + API_VERSION + "/query")
    @Headers("Content-Type: " + MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    CromwellResponse listWorkflows(@QueryMap CromwellSearch search);


    @RequestLine("GET /api/workflows/" + API_VERSION + "/{id}/status")
    CromwellStatus getStatus(@Param("id") String id);


    @RequestLine("GET /api/workflows/" + API_VERSION + "/{id}/metadata")
    CromwellMetadataResponse getMetadata(@Param("id") String id);


    @RequestLine("POST /api/workflows/" + API_VERSION + "/{id}/abort")
    CromwellStatus abortWorkflow(@Param("id") String id);


    @RequestLine("POST /api/workflows/" + API_VERSION)
    @Headers("Content-Type: " + MediaType.MULTIPART_FORM_DATA_VALUE)
    CromwellStatus createWorkflow(CromwellExecutionRequest executionRequest);


    @RequestLine("POST /api/workflows/" + API_VERSION + "/{id}/releaseHold")
    CromwellStatus releaseHold(@Param("id") String id);

}
