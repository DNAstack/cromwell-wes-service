package com.dnastack.wes.storage;


public class StorageException extends RuntimeException {

    public StorageException(Throwable e) {
        super(e);
    }

    public StorageException(String message) {
        super(message);
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }

}
