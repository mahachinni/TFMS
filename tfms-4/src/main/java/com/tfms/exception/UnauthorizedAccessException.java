package com.tfms.exception;

/**
 * Exception thrown when a user attempts to access a resource they don't have permission for
 */
public class UnauthorizedAccessException extends RuntimeException {

    private String username;
    private String resource;

    public UnauthorizedAccessException(String message) {
        super(message);
    }

    public UnauthorizedAccessException(String username, String resource) {
        super(String.format("User '%s' is not authorized to access '%s'", username, resource));
        this.username = username;
        this.resource = resource;
    }

    public UnauthorizedAccessException(String message, Throwable cause) {
        super(message, cause);
    }

    public String getUsername() {
        return username;
    }

    public String getResource() {
        return resource;
    }
}
