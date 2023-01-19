package com.marcura.service;

import com.marcura.client.FixerClient;
import com.marcura.exception.CurrencyExchangeNotFoundException;
import com.marcura.model.api.request.NewCurrencyExchange;
import com.marcura.model.api.response.CurrencyExchange;
import com.marcura.model.entity.CurrencySpread;
import com.marcura.model.entity.ExchangeRate;
import com.marcura.repository.CurrencyExchangeRateRepository;
import com.marcura.repository.CurrencySpreadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CurrencyExchangeService {
    private static final int BIG_DECIMAL_SCALE = 4;

    private final CurrencyExchangeRateRepository currencyExchangeRateRepository;
    private final CurrencySpreadRepository currencySpreadRepository;
    private final FixerClient fixerClient;

    @Value("${api.fixer.access-key}")
    private String fixerApiAccessKey;

    @Value("${base-currency}")
    private String baseCurrency;

    @Value("${spread.base}")
    private Double spreadBase;

    @Value("${spread.default}")
    private Double spreadDefault;

    public CurrencyExchange getExchangeRate(String from, String to, LocalDate date) {
        final var fromSanitized = sanitizeCurrencyName(from);
        final var toSanitized = sanitizeCurrencyName(to);
        final var exchangeRateFrom = getExchangeRate(fromSanitized, date);
        final var exchangeRateTo = getExchangeRate(toSanitized, date);
        final var spread1 = getSpread(fromSanitized);
        final var spread2 = getSpread(toSanitized);
        final var maxSpread = Double.max(spread1, spread2);
        final var exchange = exchangeRateTo.divide(exchangeRateFrom, BIG_DECIMAL_SCALE, RoundingMode.HALF_EVEN)
                                           .multiply(BigDecimal.valueOf((100 - maxSpread) / 100));
        return new CurrencyExchange(fromSanitized, toSanitized, exchange);
    }

    public void forceLatestExchangeRateRetrievalAndUpdateOrCreateExchangeRates(
            List<NewCurrencyExchange> newCurrencyExchangeList) {
        getLatestExchangeRate();

        final var saveExchangeRateList = new ArrayList<ExchangeRate>();
        for (final var newExchange : newCurrencyExchangeList) {
            final var newExchangeRate = getNewOrUpdatedExchangeRate(sanitizeCurrencyName(newExchange.from()),
                                                                    sanitizeCurrencyName(newExchange.to()),
                                                                    newExchange.date(), newExchange.exchange());
            saveExchangeRateList.add(newExchangeRate);
        }
        currencyExchangeRateRepository.saveAll(saveExchangeRateList);
    }

    @Scheduled(cron = "0 5 12 * * *", zone = "GMT")
    public void getLatestExchangeRate() {
        final var latestExchangeRate = fixerClient.getLatestExchangeRate(fixerApiAccessKey, baseCurrency);
        final var saveExchangeRateList = new ArrayList<ExchangeRate>();
        final var base = sanitizeCurrencyName(latestExchangeRate.base());
        final var date = latestExchangeRate.date();
        for (final var rate : latestExchangeRate.rates().entrySet()) {
            final var newExchangeRate = getNewOrUpdatedExchangeRate(base, sanitizeCurrencyName(rate.getKey()), date,
                                                                    rate.getValue());
            saveExchangeRateList.add(newExchangeRate);
        }
        currencyExchangeRateRepository.saveAll(saveExchangeRateList);
    }

    private BigDecimal getExchangeRate(String currency, LocalDate date) {
        if (baseCurrency.equalsIgnoreCase(currency)) {
            return BigDecimal.ONE;
        }
        return currencyExchangeRateRepository
                .findFirstByCurrencyFromAndCurrencyToAndExchangeDateLessThanEqualOrderByExchangeDateDesc(
                        baseCurrency, currency, date)
                .orElseThrow(() -> new CurrencyExchangeNotFoundException(currency, date))
                .getRate();
    }

    private Double getSpread(String currency) {
        if (baseCurrency.equalsIgnoreCase(currency)) {
            return spreadBase;
        }
        return currencySpreadRepository.findFirstByCurrencyOrderByCreatedAtDesc(currency)
                                       .map(CurrencySpread::getSpread)
                                       .orElse(spreadDefault);
    }

    private ExchangeRate getNewOrUpdatedExchangeRate(String currencyFrom, String currencyTo, LocalDate exchangeDate,
                                                     BigDecimal rate) {
        final var exchangeRate = currencyExchangeRateRepository.findByCurrencyFromAndCurrencyToAndExchangeDate(
                currencyFrom, currencyTo, exchangeDate).orElseGet(ExchangeRate::new);
        exchangeRate.setCurrencyFrom(currencyFrom);
        exchangeRate.setCurrencyTo(currencyTo);
        exchangeRate.setRate(rate);
        exchangeRate.setExchangeDate(exchangeDate);
        return exchangeRate;
    }

    private String sanitizeCurrencyName(String currency) {
        return currency.trim().toUpperCase();
    }
}
