package com.dnastack.wes.model.wes;

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
