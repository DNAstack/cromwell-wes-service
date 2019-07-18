package com.dnastack.wes.model.transfer;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Data
@ToString
public class TransferRequest {

    private String srcExternalAccountId;
    private String srcAccessToken;
    private List<List<String>> copyPairs;


    public TransferRequest(String srcAccessToken, String srcExternalAccountId) {
        this.srcExternalAccountId = srcExternalAccountId;
        this.srcAccessToken = srcAccessToken;
        copyPairs = new ArrayList<>();
    }

}
