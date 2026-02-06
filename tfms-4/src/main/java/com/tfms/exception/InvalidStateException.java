package com.tfms.exception;

import lombok.Getter;

/**
 * Exception thrown when an operation is attempted on an entity in an invalid state
 */
@Getter
public class InvalidStateException extends RuntimeException {

    private String entityName;
    private String currentState;
    private String attemptedOperation;

    public InvalidStateException(String message) {
        super(message);
    }

    public InvalidStateException(String entityName, String currentState, String attemptedOperation) {
        super(String.format("Cannot %s %s in %s state", attemptedOperation, entityName, currentState));
        this.entityName = entityName;
        this.currentState = currentState;
        this.attemptedOperation = attemptedOperation;
    }

    public InvalidStateException(String message, Throwable cause) {
        super(message, cause);
    }

}
