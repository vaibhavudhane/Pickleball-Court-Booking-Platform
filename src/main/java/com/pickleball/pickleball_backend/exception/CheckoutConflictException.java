package com.pickleball.pickleball_backend.exception;

public class CheckoutConflictException extends RuntimeException {
    public CheckoutConflictException(String message) {
        super(message);
    }
}