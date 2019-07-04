package com.dnastack.wes.client;

import com.dnastack.wes.model.transfer.ExternalAccount;
import feign.Headers;
import feign.Param;
import feign.RequestLine;
import java.util.List;

public interface ExternalAccountClient {

    @RequestLine("GET /api/externalAccount")
    @Headers({"Authorization: Bearer {accessToken}"})
    List<ExternalAccount> listExternalAccounts(@Param("accessToken") String accessToken);

}
