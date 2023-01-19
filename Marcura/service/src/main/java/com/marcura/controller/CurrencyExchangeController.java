package com.marcura.controller;

import com.marcura.exception.CurrencyExchangeDateSetToFutureException;
import com.marcura.exception.CurrencyExchangeRateIsNegativeOrZeroException;
import com.marcura.exception.CurrencyExchangeRateOverflowException;
import com.marcura.exception.CurrencyExchangeRateScaleOverflowException;
import com.marcura.exception.DuplicateCurrencyExchangeException;
import com.marcura.model.api.request.NewCurrencyExchange;
import com.marcura.model.api.response.CurrencyExchange;
import com.marcura.service.CurrencyExchangeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static com.marcura.model.entity.ExchangeRate.MAX_VALUE_LENGTH;
import static com.marcura.model.entity.ExchangeRate.SCALE;

@Slf4j
@Validated
@RestController
@RequestMapping("exchange")
@RequiredArgsConstructor
public class CurrencyExchangeController {
    private final CurrencyExchangeService currencyExchangeService;

    @GetMapping
    public CurrencyExchange getExchangeRate(@RequestParam @NotBlank String from,
                                            @RequestParam @NotBlank String to,
                                            @RequestParam(required = false)
                                            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        log.info("getExchangeRate endpoint triggered with currency from %s and to %s and date %s"
                         .formatted(from, to, date));
        validateCurrencyFromAndToAreNotDuplicate(from, to);
        validateDateIsNotInFuture(date);
        final var notNullDate = date == null ? LocalDate.now() : date;
        return currencyExchangeService.getExchangeRate(from, to, notNullDate);
    }

    @PutMapping
    public void forceLatestExchangeRateRetrievalAndUpdateOrCreateExchangeRates(
            @RequestBody @Valid List<NewCurrencyExchange> newCurrencyExchangeList) {
        log.info("forceLatestExchangeRateRetrievalAndUpdateOrCreateExchangeRates endpoint triggered for number of elements %s"
                         .formatted(newCurrencyExchangeList.size()));
        newCurrencyExchangeList.forEach(currency ->
                                                validateCurrencyFromAndToAreNotDuplicate(currency.from(), currency.to()));
        newCurrencyExchangeList.forEach(currencyExchange ->
                                                validateDateIsNotInFuture(currencyExchange.date()));
        newCurrencyExchangeList.forEach(currencyExchange ->
                                                validateExchangeRateIsNotNegative(currencyExchange.exchange()));
        newCurrencyExchangeList.forEach(currencyExchange ->
                                                validateExchangeRateNotOverflow(currencyExchange.exchange()));
        newCurrencyExchangeList.forEach(currencyExchange ->
                                                validateExchangeRateScaleNotOverflow(currencyExchange.exchange()));
        currencyExchangeService.forceLatestExchangeRateRetrievalAndUpdateOrCreateExchangeRates(newCurrencyExchangeList);
    }

    private void validateCurrencyFromAndToAreNotDuplicate(String from, String to) {
        if (from.equalsIgnoreCase(to)) {
            throw new DuplicateCurrencyExchangeException();
        }
    }

    private void validateDateIsNotInFuture(LocalDate date) {
        if (date != null && date.isAfter(LocalDate.now())) {
            throw new CurrencyExchangeDateSetToFutureException();
        }
    }

    private void validateExchangeRateIsNotNegative(BigDecimal rate) {
        if (rate.signum() != 1) {
            throw new CurrencyExchangeRateIsNegativeOrZeroException();
        }
    }

    private void validateExchangeRateNotOverflow(BigDecimal rate) {
        if (rate.precision() - rate.scale() > MAX_VALUE_LENGTH) {
            throw new CurrencyExchangeRateOverflowException();
        }
    }

    private void validateExchangeRateScaleNotOverflow(BigDecimal rate) {
        if (rate.scale() > SCALE) {
            throw new CurrencyExchangeRateScaleOverflowException();
        }
    }
}
