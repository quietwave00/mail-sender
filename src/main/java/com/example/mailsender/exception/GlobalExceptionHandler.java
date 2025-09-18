package com.example.mailsender.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<Map<String, Object>> handleCustomException(CustomException e) {
        log.error("CustomException occurred: {}", e.getMessage(), e);

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", e.getExceptionCode().getStatus().value());
        errorResponse.put("error", e.getExceptionCode().getStatus().getReasonPhrase());
        errorResponse.put("message", e.getMessage());
        errorResponse.put("code", e.getExceptionCode().name());

        return ResponseEntity
                .status(e.getExceptionCode().getStatus())
                .body(errorResponse);
    }
}
