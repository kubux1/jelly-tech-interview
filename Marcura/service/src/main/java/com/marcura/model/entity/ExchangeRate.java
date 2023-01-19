package com.marcura.model.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "currency_exchange_rate")
public class ExchangeRate {
    private static final int PRECISION = 19;
    public static final int SCALE = 6;
    public static final int MAX_VALUE_LENGTH = PRECISION - SCALE;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String currencyFrom;
    private String currencyTo;
    @Column(precision = PRECISION, scale = SCALE)
    private BigDecimal rate;
    private LocalDate exchangeDate;
    @Column(insertable = false)
    private Integer accessCounter;
    @Column(insertable = false)
    private Instant createdAt;
    @Column(insertable = false)
    private Instant updatedAt;
}
