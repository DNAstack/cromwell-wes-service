package com.dnastack.wes.client;

import com.dnastack.wes.model.wdl.WdlValidationRequest;
import com.dnastack.wes.model.wdl.WdlValidationResponse;
import feign.RequestLine;

public interface WdlValidatorClient {

    @RequestLine("POST /wdl/v1/validate")
    WdlValidationResponse validate(WdlValidationRequest validationRequest);

}
