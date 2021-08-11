package com.dnastack.wes.transfer;

public class TransferServiceDisabledException extends RuntimeException {

    public TransferServiceDisabledException() {
        super();
    }

    public TransferServiceDisabledException(String message) {
        super(message);
    }
}
