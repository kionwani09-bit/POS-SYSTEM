package com.pos.exception;
public class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }
}
