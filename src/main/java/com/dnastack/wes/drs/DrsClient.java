package com.dnastack.wes.drs;

import feign.Param;
import feign.RequestLine;

import java.net.URI;

public interface DrsClient {

    @RequestLine("GET /ga4gh/drs/v1/objects/{objectId}")
    DrsObject getObject(URI baseUri, @Param("objectId") String id);

}
