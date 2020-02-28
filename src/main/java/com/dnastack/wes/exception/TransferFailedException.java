package com.dnastack.wes.exception;

public class TransferFailedException extends RuntimeException {

    public TransferFailedException() {
        super();
    }

    public TransferFailedException(String message) {
        super(message);
    }

    public TransferFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
