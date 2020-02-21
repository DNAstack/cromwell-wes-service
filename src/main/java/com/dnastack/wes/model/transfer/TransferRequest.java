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

    private String srcAccessToken;
    private String dstAccessToken;
    private List<List<String>> copyPairs = new ArrayList<>();


    public TransferRequest(String srcAccessToken, String dstAccessToken) {
        this.srcAccessToken = srcAccessToken;
        this.dstAccessToken = dstAccessToken;
        copyPairs = new ArrayList<>();
    }

}
