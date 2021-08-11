package com.dnastack.wes.transfer;


import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@ToString
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
public class TransferJob {

    String jobId;

    List<TransferStatus> fileStatus;

    public boolean isSuccessful() {
        return isDone() && fileStatus.stream().map(TransferStatus::isSuccessful).reduce(true, Boolean::logicalAnd);
    }

    public boolean isDone() {
        return fileStatus.stream().map(TransferStatus::isDone).reduce(true, Boolean::logicalAnd);
    }


}
