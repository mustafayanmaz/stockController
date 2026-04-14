package com.musyan.stok.service;

import com.musyan.stok.dto.OrderRequestDto;
import com.musyan.stok.dto.OrderResponseDto;
import com.musyan.stok.entity.*;
import com.musyan.stok.event.StockChangedEvent;
import com.musyan.stok.exception.InsufficientStockException;
import com.musyan.stok.exception.ResourceNotFoundException;
import com.musyan.stok.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
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

    private final StockRepository stockRepository;
    private final StockTransactionRepository stockTransactionRepository;
    private final SaleOrderRepository saleOrderRepository;
    private final SaleOrderAllocationRepository saleOrderAllocationRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public OrderResponseDto createOrder(OrderRequestDto requestDto) {
        Stock stock = stockRepository.findByProductProductCodeWithLock(requestDto.getProductCode())
                .orElseThrow(() -> new ResourceNotFoundException("Stock", "productCode", requestDto.getProductCode()));

        if (requestDto.getQuantity() > stock.getQuantity()) {
            throw new InsufficientStockException(
                    "Insufficient stock for product '" + requestDto.getProductCode() + "'. Requested: "
                            + requestDto.getQuantity() + ", Available: " + stock.getQuantity()
            );
        }

        int remainingToConsume = requestDto.getQuantity();
        BigDecimal totalCost = BigDecimal.ZERO;

        List<StockTransaction> fifoLots = stockTransactionRepository
                .findByStockStockIdAndTransactionTypeAndRemainingQuantityGreaterThanOrderByTransactionDateAscTransactionIdAsc(
                        stock.getStockId(), StockTransactionType.IN, 0
                );

        int totalAvailableInLots = fifoLots.stream().mapToInt(StockTransaction::getRemainingQuantity).sum();
        if (remainingToConsume > totalAvailableInLots) {
            throw new InsufficientStockException(
                    "Insufficient FIFO lot quantity for product '" + requestDto.getProductCode() + "'. Requested: "
                            + remainingToConsume + ", Available in lots: " + totalAvailableInLots
            );
        }

        SaleOrder saleOrder = saleOrderRepository.save(SaleOrder.builder()
                .orderCode("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .product(stock.getProduct())
                .quantity(requestDto.getQuantity())
                .unitPrice(requestDto.getUnitPrice())
                .totalAmount(requestDto.getUnitPrice().multiply(BigDecimal.valueOf(requestDto.getQuantity())))
                .totalCost(BigDecimal.ZERO)
                .grossProfit(BigDecimal.ZERO)
                .orderDate(LocalDateTime.now())
                .build());

        List<SaleOrderAllocation> allocations = new ArrayList<>();

        for (StockTransaction lot : fifoLots) {
            if (remainingToConsume == 0) {
                break;
            }

            int lotRemaining = lot.getRemainingQuantity();
            int consumeQuantity = Math.min(remainingToConsume, lotRemaining);
            BigDecimal lineCost = lot.getUnitCost().multiply(BigDecimal.valueOf(consumeQuantity));

            lot.setRemainingQuantity(lotRemaining - consumeQuantity);
            remainingToConsume -= consumeQuantity;
            totalCost = totalCost.add(lineCost);

            allocations.add(SaleOrderAllocation.builder()
                    .saleOrder(saleOrder)
                    .sourceTransaction(lot)
                    .quantity(consumeQuantity)
                    .unitCost(lot.getUnitCost())
                    .lineCost(lineCost)
                    .build());
        }

        stockTransactionRepository.saveAll(fifoLots);
        saleOrderAllocationRepository.saveAll(allocations);

        BigDecimal averageConsumedCost = totalCost
                .divide(BigDecimal.valueOf(requestDto.getQuantity()), 2, RoundingMode.HALF_UP);

        stockTransactionRepository.save(StockTransaction.builder()
                .stock(stock)
                .quantity(-requestDto.getQuantity())
                .unitPrice(requestDto.getUnitPrice())
                .unitCost(averageConsumedCost)
                .remainingQuantity(0)
                .transactionType(StockTransactionType.OUT)
                .transactionDate(LocalDateTime.now())
                .build());

        stock.setQuantity(stock.getQuantity() - requestDto.getQuantity());
        stockRepository.save(stock);

        BigDecimal totalAmount = requestDto.getUnitPrice().multiply(BigDecimal.valueOf(requestDto.getQuantity()));
        saleOrder.setTotalCost(totalCost.setScale(2, RoundingMode.HALF_UP));
        saleOrder.setTotalAmount(totalAmount.setScale(2, RoundingMode.HALF_UP));
        saleOrder.setGrossProfit(totalAmount.subtract(totalCost).setScale(2, RoundingMode.HALF_UP));
        saleOrderRepository.save(saleOrder);

        eventPublisher.publishEvent(new StockChangedEvent(stock.getProduct().getProductCode()));

        return new OrderResponseDto(
                saleOrder.getOrderId(),
                saleOrder.getOrderCode(),
                stock.getProduct().getProductCode(),
                saleOrder.getQuantity(),
                saleOrder.getUnitPrice(),
                saleOrder.getTotalAmount(),
                saleOrder.getTotalCost(),
                saleOrder.getGrossProfit()
        );
    }
}
