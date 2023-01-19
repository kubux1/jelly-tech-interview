package com.marcura.repository;

import com.marcura.model.entity.CurrencySpread;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CurrencySpreadRepository extends JpaRepository<CurrencySpread, Long> {

    Optional<CurrencySpread> findFirstByCurrencyOrderByCreatedAtDesc(String currency);
}
