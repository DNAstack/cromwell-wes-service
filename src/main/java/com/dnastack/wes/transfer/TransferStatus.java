package com.dnastack.wes.transfer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

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

    public boolean isDone() {
        return status.equals("complete") || status.equals("failed") || status.equals("canceled");
    }

    public boolean isSuccessful() {
        return isDone() && status.equals("complete");
    }

}
