package com.dnastack.wes.shared;

public class FileDeletionException extends RuntimeException {

    public FileDeletionException() {
        super();
    }

    public FileDeletionException(String message) {
        super(message);
    }

    public FileDeletionException(String message, Throwable cause) {
        super(message, cause);
    }

}
