package com.marcura.model.api.request;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record NewCurrencyExchange(@NotNull @NotBlank String from, @NotNull @NotBlank String to,
                                  @NotNull BigDecimal exchange, @NotNull LocalDate date) {
}
