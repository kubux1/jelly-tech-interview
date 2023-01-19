package com.marcura.model.flixer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;

public record LatestExchangeRate(String base, LocalDate date, HashMap<String, BigDecimal> rates) {
}
