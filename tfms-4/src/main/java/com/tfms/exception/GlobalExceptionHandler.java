package com.tfms.exception;

import com.tfms.model.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.ModelAndView;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for the entire application
 * Provides centralized exception handling for controllers
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /* Helper: build ErrorResponse */
    private ErrorResponse buildErrorResponse(LocalDateTime timestamp,
                                             HttpStatus status,
                                             String error,
                                             String message,
                                             String path,
                                             Map<String, String> validationErrors) {
        return ErrorResponse.builder()
                .timestamp(timestamp)
                .status(status.value())
                .error(error)
                .message(message)
                .path(path)
                .validationErrors(validationErrors)
                .build();
    }

    /* Helper: determine if request accepts HTML */
    private boolean acceptsHtml(WebRequest request) {
        if (request instanceof ServletWebRequest sw) {
            String accept = sw.getRequest().getHeader("Accept");
            return accept != null && accept.contains("text/html");
        }
        return false;
    }

    /* Helper: respond with ModelAndView for HTML or ResponseEntity for APIs */
    private Object respond(WebRequest request, ErrorResponse errorResponse, HttpStatus status) {
        if (acceptsHtml(request)) {
            Map<String, Object> model = new HashMap<>();
            model.put("errorResponse", errorResponse);
            ModelAndView mav = new ModelAndView("access-denied", model);
            mav.setStatus(status);
            return mav;
        } else {
            return new ResponseEntity<>(errorResponse, status);
        }
    }

    /**
     * Handle ResourceNotFoundException
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public Object handleResourceNotFoundException(ResourceNotFoundException ex, WebRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());
        ErrorResponse errorResponse = buildErrorResponse(
                LocalDateTime.now(),
                HttpStatus.NOT_FOUND,
                "Resource Not Found",
                ex.getMessage(),
                request.getDescription(false).replace("uri=", ""),
                null
        );
        return respond(request, errorResponse, HttpStatus.NOT_FOUND);
    }

    /**
     * Handle InvalidStateException
     */
    @ExceptionHandler(InvalidStateException.class)
    public Object handleInvalidStateException(InvalidStateException ex, WebRequest request) {
        log.warn("Invalid state operation attempted: {}", ex.getMessage());
        ErrorResponse errorResponse = buildErrorResponse(
                LocalDateTime.now(),
                HttpStatus.CONFLICT,
                "Invalid State",
                ex.getMessage(),
                request.getDescription(false).replace("uri=", ""),
                null
        );
        return respond(request, errorResponse, HttpStatus.CONFLICT);
    }

    /**
     * Handle ValidationException
     */
    @ExceptionHandler(ValidationException.class)
    public Object handleValidationException(ValidationException ex, WebRequest request) {
        log.warn("Validation error: {}", ex.getMessage());
        ErrorResponse errorResponse = buildErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST,
                "Validation Error",
                ex.getMessage(),
                request.getDescription(false).replace("uri=", ""),
                ex.getErrors()
        );
        return respond(request, errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle UnauthorizedAccessException
     */
    @ExceptionHandler(UnauthorizedAccessException.class)
    public Object handleUnauthorizedAccessException(UnauthorizedAccessException ex, WebRequest request) {
        log.warn("Unauthorized access attempt: {}", ex.getMessage());
        ErrorResponse errorResponse = buildErrorResponse(
                LocalDateTime.now(),
                HttpStatus.FORBIDDEN,
                "Unauthorized Access",
                ex.getMessage(),
                request.getDescription(false).replace("uri=", ""),
                null
        );
        return respond(request, errorResponse, HttpStatus.FORBIDDEN);
    }

    /**
     * Handle MethodArgumentNotValidException for request body validation
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Object handleMethodArgumentNotValidException(MethodArgumentNotValidException ex, WebRequest request) {
        log.warn("Method argument validation failed");
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );

        ErrorResponse errorResponse = buildErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST,
                "Validation Error",
                "Request validation failed",
                request.getDescription(false).replace("uri=", ""),
                errors
        );
        return respond(request, errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle IllegalArgumentException
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Object handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        log.warn("Illegal argument: {}", ex.getMessage());
        ErrorResponse errorResponse = buildErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST,
                "Invalid Argument",
                ex.getMessage(),
                request.getDescription(false).replace("uri=", ""),
                null
        );
        return respond(request, errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle IllegalStateException
     */
    @ExceptionHandler(IllegalStateException.class)
    public Object handleIllegalStateException(IllegalStateException ex, WebRequest request) {
        log.warn("Illegal state: {}", ex.getMessage());
        ErrorResponse errorResponse = buildErrorResponse(
                LocalDateTime.now(),
                HttpStatus.CONFLICT,
                "Invalid State",
                ex.getMessage(),
                request.getDescription(false).replace("uri=", ""),
                null
        );
        return respond(request, errorResponse, HttpStatus.CONFLICT);
    }

    /**
     * Handle all other exceptions
     */
    @ExceptionHandler(Exception.class)
    public Object handleGlobalException(Exception ex, WebRequest request) {
        log.error("Unexpected error occurred", ex);
        ErrorResponse errorResponse = buildErrorResponse(
                LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                "An unexpected error occurred. Please try again later.",
                request.getDescription(false).replace("uri=", ""),
                null
        );
        return respond(request, errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}