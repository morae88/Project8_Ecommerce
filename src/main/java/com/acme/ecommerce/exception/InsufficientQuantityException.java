package com.acme.ecommerce.exception;

public class InsufficientQuantityException extends RuntimeException {
    public InsufficientQuantityException() {
        super("Amount requested exceeds amount in stock");
    }
}
