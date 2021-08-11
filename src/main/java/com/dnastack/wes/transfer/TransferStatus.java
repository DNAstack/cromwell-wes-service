package com.dnastack.wes.transfer;

import lombok.*;

@Getter
@Setter
@Builder
@ToString
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
public class TransferStatus {

    private String status;

    private String dstURL;

    private Long submissionTime;

    public boolean isSuccessful() {
        return isDone() && status.equals("complete");
    }

    public boolean isDone() {
        return status.equals("complete") || status.equals("failed") || status.equals("canceled");
    }

}
