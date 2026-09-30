package com.pos.exception;
public class OutOfStockException extends RuntimeException {
    public OutOfStockException(String sku) {
        super("Product out of stock: " + sku);
    }
}
