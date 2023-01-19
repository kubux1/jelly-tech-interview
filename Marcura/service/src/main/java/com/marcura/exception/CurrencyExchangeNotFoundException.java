package com.marcura.exception;

import java.time.LocalDate;

public class CurrencyExchangeNotFoundException extends RuntimeException {
    private static final String MSG = "Currency exchange not found for %s with date %s";

    public CurrencyExchangeNotFoundException(String currency, LocalDate date) {
        super(MSG.formatted(currency, date));
    }
}
