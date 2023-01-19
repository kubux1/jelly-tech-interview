package com.marcura.exception;

public class DuplicateCurrencyExchangeException extends RuntimeException {
    private static final String MSG = "Cannot exchange the same currencies";

    public DuplicateCurrencyExchangeException() {
        super(MSG);
    }
}
