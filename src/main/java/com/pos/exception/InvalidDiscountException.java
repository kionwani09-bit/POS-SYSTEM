package com.pos.exception;
public class InvalidDiscountException extends RuntimeException {
    public InvalidDiscountException(String code) {
        super("Invalid or expired discount code: " + code);
    }
}
