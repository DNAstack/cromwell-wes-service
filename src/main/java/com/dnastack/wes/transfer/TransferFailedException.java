package com.dnastack.wes.transfer;

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
