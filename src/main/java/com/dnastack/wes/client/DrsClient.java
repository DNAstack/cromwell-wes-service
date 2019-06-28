package com.dnastack.wes.client;

import com.dnastack.wes.model.drs.DrsObject;
import feign.Param;
import feign.RequestLine;
import java.net.URI;

public interface DrsClient {

    @RequestLine("GET /ga4gh/drs/v1/objects/{objectId}")
    DrsObject getObject(URI baseUri, @Param("objectId") String id);

}
