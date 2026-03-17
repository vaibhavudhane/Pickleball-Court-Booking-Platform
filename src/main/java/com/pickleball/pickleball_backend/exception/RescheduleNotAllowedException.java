package com.pickleball.pickleball_backend.exception;

public class RescheduleNotAllowedException extends RuntimeException {
    public RescheduleNotAllowedException(String message) {
        super(message);
    }
}