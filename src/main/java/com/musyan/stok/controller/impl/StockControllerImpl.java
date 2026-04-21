package com.musyan.stok.controller.impl;

import com.musyan.stok.constants.StockConstants;
import com.musyan.stok.controller.IStockController;
import com.musyan.stok.dto.ResponseDto;
import com.musyan.stok.dto.StockDto;
import com.musyan.stok.dto.StockTransactionDto;
import com.musyan.stok.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Validated
public class StockControllerImpl implements IStockController {

    private final StockService stockService;

    @Override
    public ResponseEntity<StockDto> fetchStock(@PathVariable String productCode) {
        return ResponseEntity.ok(stockService.fetchStockByProductCode(productCode));
    }

    @Override
    public ResponseEntity<ResponseDto> updateStock(
            @PathVariable String productCode,
            @RequestBody StockDto stockDto) {
        stockService.updateStock(productCode, stockDto);
        return ResponseEntity.ok(new ResponseDto(StockConstants.STATUS_200, StockConstants.MESSAGE_200));
    }

    @Override
    public ResponseEntity<ResponseDto> addStockTransaction(
            @PathVariable String productCode,
            @RequestBody StockTransactionDto transactionDto) {
        stockService.addStockTransaction(productCode, transactionDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ResponseDto(StockConstants.STATUS_201, StockConstants.MESSAGE_201));
    }

    @Override
    public ResponseEntity<ResponseDto> removeStockTransaction(
            @PathVariable String productCode,
            @RequestParam int quantity) {
        stockService.removeStockTransaction(productCode, quantity);
        return ResponseEntity.ok(new ResponseDto(StockConstants.STATUS_200, StockConstants.MESSAGE_200));
    }
}
