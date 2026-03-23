package com.musyan.stok.service;

import com.musyan.stok.dto.StockDto;
import com.musyan.stok.entity.Stock;
import com.musyan.stok.exception.ResourceNotFoundException;
import com.musyan.stok.mapper.StockMapper;
import com.musyan.stok.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StockService {

    private final StockRepository stockRepository;

    public StockDto fetchStockByProductCode(String productCode) {
        Stock stock = stockRepository.findByProductProductCode(productCode)
                .orElseThrow(() -> new ResourceNotFoundException("Stock", "productCode", productCode));

        return StockMapper.mapToStockDto(stock, new StockDto());
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
