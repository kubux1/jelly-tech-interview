package com.marcura.model.api.response;

import java.math.BigDecimal;

public record CurrencyExchange(String from, String to, BigDecimal exchange) {
}
