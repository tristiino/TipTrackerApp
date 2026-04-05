package com.tiptracker.backend.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Catches exceptions thrown anywhere in the controller layer and
 * converts them into clean JSON error responses.
 *
 * Why this instead of try/catch in every controller?
 * It keeps controller methods focused on the happy path.
 * Error handling is declared once here and applies globally.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles validation failures from TipOutService
     * (e.g. percentage splits > 100%, deductions exceeding gross tips).
     * Returns HTTP 400 Bad Request with a JSON body: { "error": "..." }
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity
                .badRequest()
                .body(Map.of("error", ex.getMessage()));
    }

    /**
     * Handles ownership/authorization failures
     * (e.g. user tries to edit another user's role or record).
     * Returns HTTP 403 Forbidden.
     */
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, String>> handleSecurityException(SecurityException ex) {
        return ResponseEntity
                .status(403)
                .body(Map.of("error", ex.getMessage()));
    }
}
