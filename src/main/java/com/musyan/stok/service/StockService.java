package com.musyan.stok.service;

import com.musyan.stok.dto.StockDto;
import com.musyan.stok.dto.StockTransactionDto;
import com.musyan.stok.entity.Stock;
import com.musyan.stok.entity.StockTransaction;
import com.musyan.stok.entity.Product;
import com.musyan.stok.exception.ResourceNotFoundException;
import com.musyan.stok.mapper.StockMapper;
import com.musyan.stok.repository.ProductRepository;
import com.musyan.stok.repository.StockRepository;
import com.musyan.stok.repository.StockTransactionRepository;
import lombok.RequiredArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StockService {

    private final StockRepository stockRepository;
    private final StockTransactionRepository stockTransactionRepository;
    private final ProductRepository productRepository;

    public StockDto fetchStockByProductCode(String productCode) {
        Stock stock = stockRepository.findByProductProductCode(productCode)
                .orElseThrow(() -> new ResourceNotFoundException("Stock", "productCode", productCode));

        return StockMapper.mapToStockDto(stock, new StockDto());
    }
    @Transactional
    public boolean addStockTransaction(String productCode, StockTransactionDto transactionDto) {
        Stock existingStock = stockRepository.findByProductProductCode(productCode)
                .orElseThrow(() -> new ResourceNotFoundException("Stock", "productCode", productCode));

        Product product = existingStock.getProduct();

        int oldQuantity = existingStock.getQuantity();
        BigDecimal oldTotalCost = product.getUnitCost().multiply(BigDecimal.valueOf(oldQuantity));

        int newQuantity = transactionDto.getQuantity();
        BigDecimal newTotalCost = transactionDto.getUnitCost().multiply(BigDecimal.valueOf(newQuantity));

        int totalQuantity = oldQuantity + newQuantity;
        if (totalQuantity > 0) {
            BigDecimal averageUnitCost = oldTotalCost.add(newTotalCost)
                    .divide(BigDecimal.valueOf(totalQuantity), 2, java.math.RoundingMode.HALF_UP);
            product.setUnitCost(averageUnitCost);
        }

        existingStock.setQuantity(totalQuantity);

        StockTransaction transaction = StockTransaction.builder()
                .stock(existingStock)
                .quantity(newQuantity)
                .unitPrice(transactionDto.getUnitPrice())
                .unitCost(transactionDto.getUnitCost())
                .transactionDate(LocalDateTime.now())
                .build();

        stockTransactionRepository.save(transaction);
        stockRepository.save(existingStock);
        productRepository.save(product);

        return true;
    }

    @Transactional
    public boolean updateStock(String productCode, StockDto stockDto) {
        Stock existingStock = stockRepository.findByProductProductCode(productCode)
                .orElseThrow(() -> new ResourceNotFoundException("Stock", "productCode", productCode));

        StockMapper.mapToStock(stockDto, existingStock);
        stockRepository.save(existingStock);
        return true;
    }
}
