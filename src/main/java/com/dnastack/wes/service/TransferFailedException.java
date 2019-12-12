package com.dnastack.wes.service;

public class TransferFailedException extends RuntimeException {

    public TransferFailedException() {
        super();
    }

    public TransferFailedException(String message) {
        super(message);
    }

}
