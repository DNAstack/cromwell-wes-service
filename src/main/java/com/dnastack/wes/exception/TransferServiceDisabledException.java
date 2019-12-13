package com.dnastack.wes.exception;

public class TransferServiceDisabledException extends RuntimeException {

    public TransferServiceDisabledException() {
        super();
    }

    public TransferServiceDisabledException(String message) {
        super(message);
    }
}
