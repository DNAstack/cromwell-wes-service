package com.dnastack.wes.exception;

import com.dnastack.wes.model.wes.ErrorResponse;
import feign.FeignException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalControllerExceptionHandler {

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ErrorResponse> handle(InvalidRequestException ex) {
        return ResponseEntity.status(404).body(ErrorResponse.builder().msg(ex.getMessage()).errorCode(400).build());
    }

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ErrorResponse> handle(FeignException ex) {
        return ResponseEntity.status(404)
            .body(ErrorResponse.builder().msg(ex.getMessage()).errorCode(ex.status()).build());
    }

    @ExceptionHandler(UnsupportedDrsAccessType.class)
    public ResponseEntity<ErrorResponse> handle(UnsupportedDrsAccessType ex) {
        return ResponseEntity.status(404).body(ErrorResponse.builder().msg(ex.getMessage()).errorCode(400).build());
    }

    @ExceptionHandler(AuthorizationException.class)
    public ResponseEntity<ErrorResponse> handle(AuthorizationException ex) {
        return ResponseEntity.status(404).body(ErrorResponse.builder().msg(ex.getMessage()).errorCode(404).build());
    }

    @ExceptionHandler(TransferFailedException.class)
    public ResponseEntity<ErrorResponse> handle(TransferFailedException ex) {
        return ResponseEntity.status(500).body(ErrorResponse.builder().msg(ex.getMessage()).errorCode(500).build());
    }
}
