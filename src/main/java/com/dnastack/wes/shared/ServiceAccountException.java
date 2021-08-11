package com.dnastack.wes.shared;

import feign.FeignException;

public class ServiceAccountException extends RuntimeException {
    public ServiceAccountException(FeignException fe) {
        super(fe.contentUTF8(), fe);
    }
}
