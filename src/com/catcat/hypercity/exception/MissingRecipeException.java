package com.catcat.hypercity.exception;

public class MissingRecipeException extends RuntimeException {
    public MissingRecipeException(String message) {
        super(message);
    }
}
