package com.pos.exception;
public class PrinterUnavailableException extends RuntimeException {
    public PrinterUnavailableException() {
        super("Receipt printer is unavailable");
    }
}
