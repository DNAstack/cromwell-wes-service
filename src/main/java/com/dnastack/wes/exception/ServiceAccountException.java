package com.dnastack.wes.exception;

import feign.FeignException;

public class ServiceAccountException extends RuntimeException {
    public ServiceAccountException(FeignException fe) {
        super(fe.contentUTF8(), fe);
    }
}
