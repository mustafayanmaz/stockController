package com.musyan.stok.mapper;

import com.musyan.stok.dto.StockDto;
import com.musyan.stok.entity.Stock;

public final class StockMapper {

    private StockMapper() {
    }

    public static StockDto mapToStockDto(Stock stock, StockDto stockDto) {
        stockDto.setStockId(stock.getStockId());
        stockDto.setQuantity(stock.getQuantity());
        stockDto.setUnit(stock.getUnit());
        stockDto.setMinimumStockLevel(stock.getMinimumStockLevel());
        return stockDto;
    }

    public static Stock mapToStock(StockDto stockDto, Stock stock) {
        stock.setQuantity(stockDto.getQuantity());
        stock.setUnit(stockDto.getUnit());
        stock.setMinimumStockLevel(stockDto.getMinimumStockLevel());
        return stock;
    }
}
