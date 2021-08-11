package com.dnastack.wes.drs;

public class UnsupportedDrsAccessType extends RuntimeException {

    public UnsupportedDrsAccessType() {
        super();
    }

    public UnsupportedDrsAccessType(String message) {
        super(message);
    }

    public UnsupportedDrsAccessType(String message, Throwable cause) {
        super(message, cause);
    }

    public UnsupportedDrsAccessType(Throwable cause) {
        super(cause);
    }

    protected UnsupportedDrsAccessType(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
