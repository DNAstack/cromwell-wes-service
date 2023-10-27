package com.dnastack.wes.api;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

public record RunFileDeletion(@JsonUnwrapped RunFile runFile, DeletionState state, @JsonUnwrapped ErrorResponse errorResponse) {

    public enum DeletionState {
        DELETED,
        ASYNC,
        FAILED
    }

}
