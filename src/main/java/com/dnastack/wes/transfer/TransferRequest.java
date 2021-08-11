package com.dnastack.wes.transfer;

import java.util.ArrayList;
import java.util.List;

import lombok.*;

@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@ToString
public class TransferRequest {

    private String srcAccessKeyId;
    private String srcAccessToken;
    private String srcSessionToken;
    private String dstAccessKeyId;
    private String dstAccessToken;
    private String dstSessionToken;
    @Builder.Default
    private List<List<String>> copyPairs = new ArrayList<>();

}
