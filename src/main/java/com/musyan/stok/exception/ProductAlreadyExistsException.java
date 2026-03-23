package com.musyan.stok.exception;

public class ProductAlreadyExistsException extends RuntimeException {

    public ProductAlreadyExistsException(String message) {
        super(message);
    }
}