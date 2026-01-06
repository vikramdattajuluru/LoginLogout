package com.bt.exception;


public class InvalidSessionException extends RuntimeException {
    

    public InvalidSessionException(String message) {
        super(message);
    }


    public InvalidSessionException(String message, Throwable cause) {
        super(message, cause);
    }
}
