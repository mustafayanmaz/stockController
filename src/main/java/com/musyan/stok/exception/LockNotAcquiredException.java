package com.musyan.stok.exception;

public class LockNotAcquiredException extends RuntimeException {
    public LockNotAcquiredException(String message) {
        super(message);
    }
}
