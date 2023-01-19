package com.marcura.exception;

import static com.marcura.model.entity.ExchangeRate.MAX_VALUE_LENGTH;

public class CurrencyExchangeRateOverflowException extends RuntimeException {
    private static final String MSG = "Currency exchange rate overflow. Max accepted value length is %s"
            .formatted(MAX_VALUE_LENGTH);

    public CurrencyExchangeRateOverflowException() {
        super(MSG);
    }
}
