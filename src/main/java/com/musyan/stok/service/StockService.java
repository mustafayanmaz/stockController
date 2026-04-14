package com.musyan.stok.service;

import com.musyan.stok.dto.StockDto;
import com.musyan.stok.dto.StockRemoveDto;
import com.musyan.stok.dto.StockTransactionDto;
import com.musyan.stok.entity.Product;
import com.musyan.stok.entity.Stock;
import com.musyan.stok.entity.StockTransaction;
import com.musyan.stok.entity.StockTransactionType;
import com.musyan.stok.event.StockChangedEvent;
import com.musyan.stok.exception.InsufficientStockException;
import com.musyan.stok.exception.ResourceNotFoundException;
import com.musyan.stok.mapper.StockMapper;
import com.musyan.stok.repository.ProductRepository;
import com.musyan.stok.repository.StockRepository;
import com.musyan.stok.repository.StockTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StockService {

    private final StockRepository stockRepository;
    private final StockTransactionRepository stockTransactionRepository;
    private final ProductRepository productRepository;
    private final ApplicationEventPublisher eventPublisher;

    public StockDto fetchStockByProductCode(String productCode) {
        Stock stock = stockRepository.findByProductProductCode(productCode)
                .orElseThrow(() -> new ResourceNotFoundException("Stock", "productCode", productCode));

        return StockMapper.mapToStockDto(stock, new StockDto());
    }
    @Transactional
    public boolean addStockTransaction(String productCode, StockTransactionDto transactionDto) {
        if (!productRepository.existsByProductCode(productCode)) {
            throw new ResourceNotFoundException("Product", "productCode", productCode);
        }
        Stock existingStock = stockRepository.findByProductProductCodeWithLock(productCode)
                .orElseThrow(() -> new ResourceNotFoundException("Stock", "productCode", productCode));

        int newQuantity = transactionDto.getQuantity();

        existingStock.setQuantity(existingStock.getQuantity() + newQuantity);

        StockTransaction transaction = StockTransaction.builder()
                .stock(existingStock)
                .quantity(newQuantity)
                .unitPrice(transactionDto.getUnitPrice())
                .unitCost(transactionDto.getUnitCost())
                .remainingQuantity(newQuantity)
                .transactionType(StockTransactionType.IN)
                .transactionDate(LocalDateTime.now())
                .build();

        stockTransactionRepository.save(transaction);
        stockRepository.save(existingStock);
        eventPublisher.publishEvent(new StockChangedEvent(productCode));

        return true;
    }

    @Transactional
    public boolean removeStockTransaction(String productCode, StockRemoveDto removeDto) {
        Stock existingStock = stockRepository.findByProductProductCodeWithLock(productCode)
                .orElseThrow(() -> new ResourceNotFoundException("Stock", "productCode", productCode));

        int removeQuantity = removeDto.getQuantity();
        int currentQuantity = existingStock.getQuantity();

        if (removeQuantity > currentQuantity) {
            throw new InsufficientStockException(
                    "Insufficient stock for product '" + productCode + "'. " +
                    "Requested: " + removeQuantity + ", Available: " + currentQuantity
            );
        }

        List<StockTransaction> fifoLots = stockTransactionRepository
                .findByStockStockIdAndTransactionTypeAndRemainingQuantityGreaterThanOrderByTransactionDateAscTransactionIdAsc(
                        existingStock.getStockId(), StockTransactionType.IN, 0
                );

        int totalAvailableInLots = fifoLots.stream().mapToInt(StockTransaction::getRemainingQuantity).sum();
        if (removeQuantity > totalAvailableInLots) {
            throw new InsufficientStockException(
                    "Insufficient FIFO lot quantity for product '" + productCode + "'. " +
                    "Requested: " + removeQuantity + ", Available in lots: " + totalAvailableInLots
            );
        }

        int remainingToConsume = removeQuantity;
        BigDecimal totalConsumedCost = BigDecimal.ZERO;

        for (StockTransaction lot : fifoLots) {
            if (remainingToConsume == 0) {
                break;
            }

            int lotRemaining = lot.getRemainingQuantity();
            int consumeQuantity = Math.min(remainingToConsume, lotRemaining);
            lot.setRemainingQuantity(lotRemaining - consumeQuantity);

            totalConsumedCost = totalConsumedCost.add(
                    lot.getUnitCost().multiply(BigDecimal.valueOf(consumeQuantity))
            );
            remainingToConsume -= consumeQuantity;
        }

        stockTransactionRepository.saveAll(fifoLots);

        existingStock.setQuantity(currentQuantity - removeQuantity);

        BigDecimal consumedAverageCost = totalConsumedCost
                .divide(BigDecimal.valueOf(removeQuantity), 2, RoundingMode.HALF_UP);

        StockTransaction transaction = StockTransaction.builder()
                .stock(existingStock)
                .quantity(-removeQuantity)
                .unitPrice(BigDecimal.ZERO)
                .unitCost(consumedAverageCost)
                .remainingQuantity(0)
                .transactionType(StockTransactionType.OUT)
                .transactionDate(LocalDateTime.now())
                .build();

        stockTransactionRepository.save(transaction);
        stockRepository.save(existingStock);
        eventPublisher.publishEvent(new StockChangedEvent(productCode));

        return true;
    }

    @Transactional
    public boolean updateStock(String productCode, StockDto stockDto) {
        Stock existingStock = stockRepository.findByProductProductCodeWithLock(productCode)
                .orElseThrow(() -> new ResourceNotFoundException("Stock", "productCode", productCode));

                int currentQuantity = existingStock.getQuantity();
                int targetQuantity = stockDto.getQuantity();
                int diff = targetQuantity - currentQuantity;

                if (diff > 0) {
                        Product product = existingStock.getProduct();

                        StockTransaction adjustIn = StockTransaction.builder()
                                        .stock(existingStock)
                                        .quantity(diff)
                                        .unitPrice(product.getUnitCost())
                                        .unitCost(product.getUnitCost())
                                        .remainingQuantity(diff)
                                        .transactionType(StockTransactionType.IN)
                                        .transactionDate(LocalDateTime.now())
                                        .build();
                        stockTransactionRepository.save(adjustIn);
                } else if (diff < 0) {
                        StockRemoveDto removeDto = new StockRemoveDto();
                        removeDto.setQuantity(-diff);
                        removeStockTransaction(productCode, removeDto);
                }

                existingStock.setQuantity(targetQuantity);
                existingStock.setUnit(stockDto.getUnit());
                existingStock.setMinimumStockLevel(stockDto.getMinimumStockLevel());
        stockRepository.save(existingStock);
                eventPublisher.publishEvent(new StockChangedEvent(productCode));
        return true;
    }
}
