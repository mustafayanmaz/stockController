package com.musyan.stok.repository;

import com.musyan.stok.entity.Stock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StockRepository extends JpaRepository<Stock, Long> {

    Optional<Stock> findByProductProductId(Long productId);

    Optional<Stock> findByProductProductCode(String productCode);
}
