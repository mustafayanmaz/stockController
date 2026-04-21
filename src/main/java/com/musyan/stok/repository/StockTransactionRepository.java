package com.musyan.stok.repository;

import com.musyan.stok.entity.StockTransaction;
import com.musyan.stok.entity.StockTransactionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockTransactionRepository extends JpaRepository<StockTransaction, Long> {

    List<StockTransaction> findByProductProductIdAndTransactionTypeAndRemainingQuantityGreaterThanOrderByTransactionDateAscTransactionIdAsc(
            Long productId,
            StockTransactionType transactionType,
            Integer remainingQuantity
    );
}
