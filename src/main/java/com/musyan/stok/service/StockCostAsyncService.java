package com.musyan.stok.service;

import com.musyan.stok.config.RabbitMQConfig;
import com.musyan.stok.entity.StockTransaction;
import com.musyan.stok.entity.StockTransactionType;
import com.musyan.stok.event.StockCostMessage;
import com.musyan.stok.repository.ProductRepository;
import com.musyan.stok.repository.StockTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StockCostAsyncService {

    private final StockTransactionRepository stockTransactionRepository;
    private final ProductRepository productRepository;

    @RabbitListener(queues = RabbitMQConfig.QUEUE)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onStockChanged(StockCostMessage message) {
        Long productId = productRepository.findByProductCode(message.productCode())
                .map(p -> p.getProductId())
                .orElse(null);
        if (productId == null) return;

        List<StockTransaction> activeLots = stockTransactionRepository
                .findByProductProductIdAndTransactionTypeAndRemainingQuantityGreaterThanOrderByTransactionDateAscTransactionIdAsc(
                        productId, StockTransactionType.IN, 0
                );

        int totalRemainingQuantity = activeLots.stream().mapToInt(StockTransaction::getRemainingQuantity).sum();

        BigDecimal newAverageCost = BigDecimal.ZERO;
        if (totalRemainingQuantity > 0) {
            BigDecimal totalRemainingCost = activeLots.stream()
                    .map(lot -> lot.getUnitCost().multiply(BigDecimal.valueOf(lot.getRemainingQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            newAverageCost = totalRemainingCost.divide(BigDecimal.valueOf(totalRemainingQuantity), 2, RoundingMode.HALF_UP);
        }

        productRepository.updateUnitCost(message.productCode(), newAverageCost);
    }
}

