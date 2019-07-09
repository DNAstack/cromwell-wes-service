package com.dnastack.wes.client;

import com.dnastack.wes.model.wdl.WdlValidationRequest;
import com.dnastack.wes.model.wdl.WdlValidationResponse;
import feign.Headers;
import feign.Param;
import feign.RequestLine;

public interface WdlValidatorClient {

    @RequestLine("POST /wdl/v1/validate")
    @Headers({"Authorization: Bearer {accessToken}"})
    WdlValidationResponse validate(WdlValidationRequest validationRequest, @Param("accessToken") String accessToken);

}
