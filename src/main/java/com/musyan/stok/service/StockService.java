package com.musyan.stok.service;

import com.musyan.stok.dto.StockDto;
import com.musyan.stok.dto.StockTransactionDto;
import com.musyan.stok.entity.Product;
import com.musyan.stok.entity.StockTransaction;
import com.musyan.stok.entity.StockTransactionType;
import com.musyan.stok.event.StockCostMessage;
import com.musyan.stok.exception.InsufficientStockException;
import com.musyan.stok.exception.ResourceNotFoundException;
import com.musyan.stok.repository.ProductRepository;
import com.musyan.stok.repository.StockTransactionRepository;
import com.musyan.stok.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StockService {

    private final ProductRepository productRepository;
    private final StockTransactionRepository stockTransactionRepository;
    private final RabbitTemplate rabbitTemplate;

    public StockDto fetchStockByProductCode(String productCode) {
        Product product = productRepository.findByProductCode(productCode)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productCode", productCode));

        StockDto stockDto = new StockDto();
        stockDto.setQuantity(product.getQuantity());
        stockDto.setUnit(product.getUnit());
        return stockDto;
    }

    @Transactional
    public boolean addStockTransaction(String productCode, StockTransactionDto transactionDto) {
        Product product = productRepository.findByProductCodeWithLock(productCode)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productCode", productCode));

        int newQuantity = transactionDto.getQuantity();
        product.setQuantity(product.getQuantity() + newQuantity);

        StockTransaction transaction = StockTransaction.builder()
                .product(product)
                .quantity(newQuantity)
                .unitCost(transactionDto.getUnitCost())
                .remainingQuantity(newQuantity)
                .transactionType(StockTransactionType.IN)
                .transactionDate(LocalDateTime.now())
                .build();

        stockTransactionRepository.save(transaction);
        productRepository.save(product);
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.ROUTING_KEY, new StockCostMessage(productCode));
        return true;
    }

    @Transactional
    public boolean removeStockTransaction(String productCode, int quantity) {
        Product product = productRepository.findByProductCodeWithLock(productCode)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productCode", productCode));

        int currentQuantity = product.getQuantity();
        if (quantity > currentQuantity) {
            throw new InsufficientStockException(
                    "Insufficient stock for product '" + productCode + "'. "
                            + "Requested: " + quantity + ", Available: " + currentQuantity
            );
        }

        List<StockTransaction> fifoLots = stockTransactionRepository
                .findByProductProductIdAndTransactionTypeAndRemainingQuantityGreaterThanOrderByTransactionDateAscTransactionIdAsc(
                        product.getProductId(), StockTransactionType.IN, 0
                );

        int totalAvailableInLots = fifoLots.stream().mapToInt(StockTransaction::getRemainingQuantity).sum();
        if (quantity > totalAvailableInLots) {
            throw new InsufficientStockException(
                    "Insufficient FIFO lot quantity for product '" + productCode + "'. "
                            + "Requested: " + quantity + ", Available in lots: " + totalAvailableInLots
            );
        }

        int remainingToConsume = quantity;
        BigDecimal totalConsumedCost = BigDecimal.ZERO;

        for (StockTransaction lot : fifoLots) {
            if (remainingToConsume == 0) break;
            int consumeQuantity = Math.min(remainingToConsume, lot.getRemainingQuantity());
            lot.setRemainingQuantity(lot.getRemainingQuantity() - consumeQuantity);
            totalConsumedCost = totalConsumedCost.add(lot.getUnitCost().multiply(BigDecimal.valueOf(consumeQuantity)));
            remainingToConsume -= consumeQuantity;
        }

        stockTransactionRepository.saveAll(fifoLots);
        product.setQuantity(currentQuantity - quantity);

        BigDecimal averageCost = totalConsumedCost.divide(BigDecimal.valueOf(quantity), 2, RoundingMode.HALF_UP);
        stockTransactionRepository.save(StockTransaction.builder()
                .product(product)
                .quantity(-quantity)
                .unitCost(averageCost)
                .remainingQuantity(0)
                .transactionType(StockTransactionType.OUT)
                .transactionDate(LocalDateTime.now())
                .build());

        productRepository.save(product);
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.ROUTING_KEY, new StockCostMessage(productCode));
        return true;
    }

    @Transactional
    public boolean updateStock(String productCode, StockDto stockDto) {
        Product product = productRepository.findByProductCodeWithLock(productCode)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productCode", productCode));

        int diff = stockDto.getQuantity() - product.getQuantity();
        if (diff > 0) {
            StockTransaction adjustIn = StockTransaction.builder()
                    .product(product)
                    .quantity(diff)
                    .unitCost(product.getUnitCost())
                    .remainingQuantity(diff)
                    .transactionType(StockTransactionType.IN)
                    .transactionDate(LocalDateTime.now())
                    .build();
            stockTransactionRepository.save(adjustIn);
        } else if (diff < 0) {
            removeStockTransaction(productCode, -diff);
        }

        product.setQuantity(stockDto.getQuantity());
        product.setUnit(stockDto.getUnit());
        productRepository.save(product);
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.ROUTING_KEY, new StockCostMessage(productCode));
        return true;
    }
}
