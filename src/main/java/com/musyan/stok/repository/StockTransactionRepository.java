package com.musyan.stok.repository;

import com.musyan.stok.entity.StockTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockTransactionRepository extends JpaRepository<StockTransaction, Long> {
}
