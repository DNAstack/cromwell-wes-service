package com.dnastack.wes.v1.client;

import com.dnastack.wes.v1.model.wdl.WdlValidationRequest;
import com.dnastack.wes.v1.model.wdl.WdlValidationResponse;
import feign.RequestLine;

public interface WdlValidatorClient {

    @RequestLine("POST /wdl/v1/validate")
    WdlValidationResponse validate(WdlValidationRequest validationRequest);

}
