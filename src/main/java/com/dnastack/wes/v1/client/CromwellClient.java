package com.dnastack.wes.v1.client;

import com.dnastack.wes.v1.model.cromwell.CromwellExecutionRequest;
import com.dnastack.wes.v1.model.cromwell.CromwellMetadataResponse;
import com.dnastack.wes.v1.model.cromwell.CromwellResponse;
import com.dnastack.wes.v1.model.cromwell.CromwellSearch;
import com.dnastack.wes.v1.model.cromwell.CromwellStatus;
import feign.Headers;
import feign.Param;
import feign.QueryMap;
import feign.RequestLine;
import org.springframework.http.MediaType;


public interface CromwellClient {

    String API_VERSION = "v1";

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
