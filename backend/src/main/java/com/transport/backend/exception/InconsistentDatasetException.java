package com.transport.backend.exception;

/**
 * Thrown when persistent data in the database fails structural completeness,
 * referential integrity, capacity bounds, or operational lifecycle invariants.
 */
public class InconsistentDatasetException extends RuntimeException {

    public InconsistentDatasetException(String message) {
        super(message);
    }

    public InconsistentDatasetException(String message, Throwable cause) {
        super(message, cause);
    }
}
