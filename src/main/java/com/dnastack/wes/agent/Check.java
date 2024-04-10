package com.dnastack.wes.agent;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Check {

    private CheckType type;
    private CheckOutcome outcome;
    private String error;

    public enum CheckType {
        CONNECTIVITY,
        CREDENTIALS,
        PERMISSIONS,
        STORAGE,
        LOGS
    }

    public enum CheckOutcome {
        SUCCESS,
        FAILURE
    }

}
