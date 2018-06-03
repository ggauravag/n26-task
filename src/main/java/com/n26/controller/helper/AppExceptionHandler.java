package com.n26.controller.helper;

import com.n26.repository.NoRecordedTransactionException;
import com.n26.service.StaleTransactionException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class AppExceptionHandler {

    @ExceptionHandler(StaleTransactionException.class)
    public ResponseEntity handleStaleTransactionException(StaleTransactionException ex) {
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(NoRecordedTransactionException.class)
    public ResponseEntity handleNoRecordedTransactionException(NoRecordedTransactionException ex) {
        return ResponseEntity.notFound().build();
    }

}
