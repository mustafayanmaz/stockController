package com.musyan.stok.service;

import com.musyan.stok.entity.Product;
import com.musyan.stok.entity.Stock;
import com.musyan.stok.entity.StockTransaction;
import com.musyan.stok.entity.StockTransactionType;
import com.musyan.stok.event.CostIncreasedEvent;
import com.musyan.stok.event.StockChangedEvent;
import com.musyan.stok.repository.ProductRepository;
import com.musyan.stok.repository.StockRepository;
import com.musyan.stok.repository.StockTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StockCostAsyncService {

    private final StockRepository stockRepository;
    private final StockTransactionRepository stockTransactionRepository;
    private final ProductRepository productRepository;
    private final CostIncreaseHandler costIncreaseHandler;
    private final ApplicationEventPublisher eventPublisher;

    @Async
    @EventListener
    @Transactional
    public void onStockChanged(StockChangedEvent event) {
        Stock stock = stockRepository.findByProductProductCode(event.productCode()).orElse(null);
        if (stock == null) {
            return;
        }

        Product product = stock.getProduct();
        BigDecimal oldAverageCost = product.getUnitCost();

        List<StockTransaction> activeLots = stockTransactionRepository
                .findByStockStockIdAndTransactionTypeAndRemainingQuantityGreaterThanOrderByTransactionDateAscTransactionIdAsc(
                        stock.getStockId(), StockTransactionType.IN, 0
                );

        int totalRemainingQuantity = activeLots.stream().mapToInt(StockTransaction::getRemainingQuantity).sum();

        BigDecimal newAverageCost = BigDecimal.ZERO;
        if (totalRemainingQuantity > 0) {
            BigDecimal totalRemainingCost = activeLots.stream()
                    .map(lot -> lot.getUnitCost().multiply(BigDecimal.valueOf(lot.getRemainingQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            newAverageCost = totalRemainingCost
                    .divide(BigDecimal.valueOf(totalRemainingQuantity), 2, RoundingMode.HALF_UP);
        }

        product.setUnitCost(newAverageCost);
        productRepository.save(product);

        if (newAverageCost.compareTo(oldAverageCost) > 0) {
            costIncreaseHandler.onCostIncreased(product.getProductCode(), oldAverageCost, newAverageCost);
            eventPublisher.publishEvent(new CostIncreasedEvent(product.getProductCode(), oldAverageCost, newAverageCost));
        }
    }
}
