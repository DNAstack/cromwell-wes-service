package com.dnastack.wes.v1.model.wes;

public enum State {

    UNKNOWN,
    QUEUED,
    INITIALIZING,
    RUNNING,
    PAUSED,
    COMPLETE,
    EXECUTOR_ERROR,
    SYSTEM_ERROR,
    CANCELED,
    CANCELINGSTATE
}
