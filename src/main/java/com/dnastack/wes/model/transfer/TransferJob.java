package com.dnastack.wes.model.transfer;


import java.util.List;
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
public class TransferJob {

    String jobId;

    List<TransferStatus> fileStatus;


    public boolean isDone() {
        return fileStatus.stream().map(TransferStatus::isDone).reduce(true, Boolean::logicalAnd);
    }

    public boolean isSuccessful() {
        return isDone() && fileStatus.stream().map(TransferStatus::isSuccessful).reduce(true, Boolean::logicalAnd);
    }


}
