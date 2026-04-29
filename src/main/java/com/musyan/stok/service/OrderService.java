package com.musyan.stok.service;

import com.musyan.stok.dto.OrderRequestDto;
import com.musyan.stok.dto.OrderResponseDto;
import com.musyan.stok.entity.Product;
import com.musyan.stok.entity.SaleOrder;
import com.musyan.stok.entity.SaleOrderAllocation;
import com.musyan.stok.entity.StockTransaction;
import com.musyan.stok.entity.StockTransactionType;
import com.musyan.stok.event.StockCostMessage;
import com.musyan.stok.exception.InsufficientStockException;
import com.musyan.stok.exception.ResourceNotFoundException;
import com.musyan.stok.repository.ProductRepository;
import com.musyan.stok.repository.SaleOrderAllocationRepository;
import com.musyan.stok.repository.SaleOrderRepository;
import com.musyan.stok.repository.StockTransactionRepository;
import com.musyan.stok.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final ProductRepository productRepository;
    private final StockTransactionRepository stockTransactionRepository;
    private final SaleOrderRepository saleOrderRepository;
        private final SaleOrderAllocationRepository saleOrderAllocationRepository;
    private final RabbitTemplate rabbitTemplate;

    @Transactional
    public OrderResponseDto createOrder(OrderRequestDto requestDto) {
        Product product = productRepository.findByProductCodeWithLock(requestDto.getProductCode())
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productCode", requestDto.getProductCode()));

        if (requestDto.getQuantity() > product.getQuantity()) {
            throw new InsufficientStockException(
                    "Insufficient stock for product '" + requestDto.getProductCode() + "'. Requested: "
                            + requestDto.getQuantity() + ", Available: " + product.getQuantity()
            );
        }

        List<StockTransaction> fifoLots = stockTransactionRepository
                .findByProductProductIdAndTransactionTypeAndRemainingQuantityGreaterThanOrderByTransactionDateAscTransactionIdAsc(
                        product.getProductId(), StockTransactionType.IN, 0
                );

        int totalAvailableInLots = fifoLots.stream().mapToInt(StockTransaction::getRemainingQuantity).sum();
        if (requestDto.getQuantity() > totalAvailableInLots) {
            throw new InsufficientStockException(
                    "Insufficient FIFO lot quantity for product '" + requestDto.getProductCode() + "'. Requested: "
                            + requestDto.getQuantity() + ", Available in lots: " + totalAvailableInLots
            );
        }

        int remainingToConsume = requestDto.getQuantity();
        BigDecimal totalCost = BigDecimal.ZERO;

        BigDecimal totalAmount = requestDto.getUnitPrice()
                .multiply(BigDecimal.valueOf(requestDto.getQuantity()))
                .setScale(2, RoundingMode.HALF_UP);

        SaleOrder saleOrder = saleOrderRepository.save(SaleOrder.builder()
                .orderCode("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .product(product)
                .quantity(requestDto.getQuantity())
                .unitPrice(requestDto.getUnitPrice())
                .totalAmount(totalAmount)
                .totalCost(BigDecimal.ZERO)
                .orderDate(LocalDateTime.now())
                .build());

        List<SaleOrderAllocation> allocations = new ArrayList<>();

        for (StockTransaction lot : fifoLots) {
            if (remainingToConsume == 0) {
                break;
            }

            int consumeQuantity = Math.min(remainingToConsume, lot.getRemainingQuantity());
            totalCost = totalCost.add(lot.getUnitCost().multiply(BigDecimal.valueOf(consumeQuantity)));
            lot.setRemainingQuantity(lot.getRemainingQuantity() - consumeQuantity);
            remainingToConsume -= consumeQuantity;

            allocations.add(SaleOrderAllocation.builder()
                    .saleOrder(saleOrder)
                    .sourceTransaction(lot)
                    .quantity(consumeQuantity)
                    .unitCost(lot.getUnitCost())
                    .build());
        }

        stockTransactionRepository.saveAll(fifoLots);
        saleOrderAllocationRepository.saveAll(allocations);

        BigDecimal averageConsumedCost = totalCost
                .divide(BigDecimal.valueOf(requestDto.getQuantity()), 2, RoundingMode.HALF_UP);

        stockTransactionRepository.save(StockTransaction.builder()
                .product(product)
                .quantity(-requestDto.getQuantity())
                .unitCost(averageConsumedCost)
                .remainingQuantity(0)
                .transactionType(StockTransactionType.OUT)
                .transactionDate(LocalDateTime.now())
                .build());

        product.setQuantity(product.getQuantity() - requestDto.getQuantity());
        productRepository.save(product);

        saleOrder.setTotalCost(totalCost.setScale(2, RoundingMode.HALF_UP));
        saleOrderRepository.save(saleOrder);

        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.ROUTING_KEY, new StockCostMessage(product.getProductCode()));

        return new OrderResponseDto(
                saleOrder.getOrderId(),
                saleOrder.getOrderCode(),
                product.getProductCode(),
                saleOrder.getQuantity(),
                saleOrder.getUnitPrice(),
                saleOrder.getTotalAmount(),
                saleOrder.getTotalCost()
        );
    }
}
