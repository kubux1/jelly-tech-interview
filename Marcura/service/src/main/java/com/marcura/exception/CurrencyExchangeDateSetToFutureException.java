package com.marcura.exception;

public class CurrencyExchangeDateSetToFutureException extends RuntimeException {
    private static final String MSG = "Currency exchange date cannot be set to future";

    public CurrencyExchangeDateSetToFutureException() {
        super(MSG);
    }
}
