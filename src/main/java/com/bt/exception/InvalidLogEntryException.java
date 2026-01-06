package com.bt.exception;

/**
 * Exception thrown when an invalid log entry is encountered during processing.
 */
public class InvalidLogEntryException extends Exception {
    public InvalidLogEntryException(String message) {
        super(message);
    }

    public InvalidLogEntryException(String message, Throwable cause) {
        super(message, cause);
    }
}
