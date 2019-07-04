package com.dnastack.wes.client;

import com.dnastack.wes.model.transfer.TransferJob;
import com.dnastack.wes.model.transfer.TransferRequest;
import feign.Headers;
import feign.Param;
import feign.RequestLine;
import org.springframework.http.MediaType;

public interface TransferServiceClient {


    @RequestLine("POST /api/admin/transfer?userId={userId}")
    @Headers("Content-Type: " + MediaType.APPLICATION_JSON_VALUE)
    TransferJob submitTransferRequest( TransferRequest transferRequest,@Param("userId") String userId);

    @RequestLine("GET /api/admin/transfer/{transferId}")
    TransferJob getTransferJob(@Param("transferId") String transferId);

}
