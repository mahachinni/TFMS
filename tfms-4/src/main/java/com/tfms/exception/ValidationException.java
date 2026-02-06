package com.tfms.exception;

import java.util.HashMap;
import java.util.Map;

/**
 * Exception thrown when validation fails
 */
public class ValidationException extends RuntimeException {

    private Map<String, String> errors;

    public ValidationException(String message) {
        super(message);
        this.errors = new HashMap<>();
    }

    public ValidationException(String message, Map<String, String> errors) {
        super(message);
        this.errors = errors;
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
        this.errors = new HashMap<>();
    }

    public Map<String, String> getErrors() {
        return errors;
    }

    public void addError(String field, String message) {
        errors.put(field, message);
    }
}

