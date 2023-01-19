package com.marcura.exception;

public class CurrencyExchangeRateIsNegativeOrZeroException extends RuntimeException {
    private static final String MSG = "Currency exchange rate cannot be negative or zero";

    public CurrencyExchangeRateIsNegativeOrZeroException() {
        super(MSG);
    }
}
