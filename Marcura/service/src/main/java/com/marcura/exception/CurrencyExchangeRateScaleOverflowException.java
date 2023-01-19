package com.marcura.exception;

import static com.marcura.model.entity.ExchangeRate.SCALE;

public class CurrencyExchangeRateScaleOverflowException extends RuntimeException {
    private static final String MSG = "Currency exchange rate scale overflow. Max accepted length is %s".formatted(SCALE);

    public CurrencyExchangeRateScaleOverflowException() {
        super(MSG);
    }
}
