package com.dnastack.wes.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

public record RunFileDeletion(@JsonUnwrapped RunFile runFile, DeletionState state, @JsonProperty("error_response") ErrorResponse errorResponse) {

    public enum DeletionState {
        DELETED,
        ASYNC,
        FAILED
    }

}
