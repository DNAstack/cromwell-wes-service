package com.dnastack.wes.shared;

import com.dnastack.wes.api.ErrorResponse;
import com.dnastack.wes.api.RangeNotSatisfiableException;
import com.dnastack.wes.workflow.UnauthorizedWorkflowException;
import feign.FeignException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.io.FileNotFoundException;

@ControllerAdvice
public class GlobalControllerExceptionHandler {

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ErrorResponse> handle(InvalidRequestException ex) {
        return ResponseEntity.status(400).body(ErrorResponse.builder().msg(ex.getMessage()).errorCode(400).build());
    }

    @ExceptionHandler(FileNotFoundException.class)
    public ResponseEntity<ErrorResponse> handle(FileNotFoundException ex) {
        return ResponseEntity.status(404).body(ErrorResponse.builder().msg(ex.getMessage()).errorCode(404).build());
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handle(NotFoundException ex) {
        return ResponseEntity.status(404).body(ErrorResponse.builder().msg(ex.getMessage()).errorCode(404).build());
    }

    @ExceptionHandler({ AccessDeniedException.class, UnauthorizedWorkflowException.class })
    public ResponseEntity<ErrorResponse> handle(Exception ex) {
        return ResponseEntity.status(403).body(ErrorResponse.builder().msg(ex.getMessage()).errorCode(403).build());
    }

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ErrorResponse> handle(FeignException ex) {
        return ResponseEntity.status(ex.status())
            .body(ErrorResponse.builder().msg(ex.getMessage()).errorCode(ex.status()).build());
    }

    @ExceptionHandler(RangeNotSatisfiableException.class)
    public ResponseEntity<ErrorResponse> handle(RangeNotSatisfiableException ex) {
        return ResponseEntity.status(416)
            .body(ErrorResponse.builder().msg(ex.getMessage()).errorCode(416).build());
    }

}
