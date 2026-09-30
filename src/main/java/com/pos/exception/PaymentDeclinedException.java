package com.pos.exception;
public class PaymentDeclinedException extends RuntimeException {
    public PaymentDeclinedException(String reason) {
        super("Payment declined: " + reason);
    }
}
