package com.marcura.repository;

import com.marcura.model.entity.ExchangeRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface CurrencyExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {
    @Query(value =
            "UPDATE currency_exchange_rate SET access_counter = access_counter + 1 " +
                    "WHERE id = (" +
                    "SELECT id FROM currency_exchange_rate " +
                    "WHERE currency_from = :currencyFrom AND currency_to = :currencyTo AND exchange_date <= :exchangeDate " +
                    "ORDER BY exchange_date DESC LIMIT 1) " +
                    "RETURNING *", nativeQuery = true)
    Optional<ExchangeRate> findFirstByCurrencyFromAndCurrencyToAndExchangeDateLessThanEqualOrderByExchangeDateDesc(
            @Param("currencyFrom") String currencyFrom,
            @Param("currencyTo") String currencyTo,
            @Param("exchangeDate") LocalDate exchangeDate);

    Optional<ExchangeRate> findByCurrencyFromAndCurrencyToAndExchangeDate(
            @Param("currencyFrom") String currencyFrom,
            @Param("currencyTo") String currencyTo,
            @Param("exchangeDate") LocalDate exchangeDate);

}
