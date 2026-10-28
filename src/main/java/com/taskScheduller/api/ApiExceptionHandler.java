
package com.taskScheduller.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> badRequest(
            IllegalArgumentException e
    ) {
        return ResponseEntity.badRequest()
                .body(Map.of("error", String.valueOf(e.getMessage())));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validationFailed(
            MethodArgumentNotValidException e
    ) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();

        for (FieldError error : e.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }

        Map<String, Object> body = Map.of(
                "error", "Validation failed",
                "fields", fieldErrors
        );

        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(RejectedExecutionException.class)
    public ResponseEntity<Map<String, String>> executionLimitReached(
            RejectedExecutionException e
    ) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "Execution capacity reached"));
    }
}
