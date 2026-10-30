package com.deepblue.rescue.exception;

import com.deepblue.rescue.dto.response.ErrorResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex) {

        return build(HttpStatus.NOT_FOUND, ex.getMessage(), Map.of());
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRule(
            BusinessRuleException ex) {

        return build(HttpStatus.CONFLICT, ex.getMessage(), Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex) {

        Map<String, String> details = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        error -> String.valueOf(error.getDefaultMessage()),
                        (first, second) -> first
                ));

        return build(HttpStatus.BAD_REQUEST, "Request validation failed", details);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMessageNotReadable(
            HttpMessageNotReadableException ex) {

        return build(
                HttpStatus.BAD_REQUEST,
                "Malformed or invalid JSON request",
                Map.of("body", "Check JSON syntax and enum values"));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex) {

        Map<String, String> details = Map.of(
                ex.getName(),
                "Invalid value: " + String.valueOf(ex.getValue()));

        return build(HttpStatus.BAD_REQUEST, "Invalid request parameter", details);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(
            MissingServletRequestParameterException ex) {

        return build(
                HttpStatus.BAD_REQUEST,
                "Missing request parameter",
                Map.of(ex.getParameterName(), "Parameter is required"));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex) {

        return build(
                HttpStatus.METHOD_NOT_ALLOWED,
                "HTTP method not supported: " + ex.getMethod(),
                Map.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(
            NoResourceFoundException ex) {

        return build(HttpStatus.NOT_FOUND, "Endpoint not found", Map.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(
            Exception ex) {

        // El detalle queda solo en el log del servidor; el cliente no ve nada interno.
        log.error("Unexpected error", ex);

        return build(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred",
                Map.of());
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status,
                                                String message,
                                                Map<String, String> details) {

        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                details
        );

        return ResponseEntity.status(status).body(error);
    }
}
