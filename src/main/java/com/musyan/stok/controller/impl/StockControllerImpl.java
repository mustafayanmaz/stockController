package com.musyan.stok.controller.impl;

import com.musyan.stok.constants.StockConstants;
import com.musyan.stok.controller.IStockController;
import com.musyan.stok.dto.ResponseDto;
import com.musyan.stok.dto.StockDto;
import com.musyan.stok.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "/api/stocks")
@RequiredArgsConstructor
@Validated
public class StockControllerImpl implements IStockController {

    private final StockService stockService;

    @Override
    @GetMapping("/{productCode}")
    public ResponseEntity<StockDto> fetchStock(@PathVariable String productCode) {
        StockDto stockDto = stockService.fetchStockByProductCode(productCode);
        return ResponseEntity.status(HttpStatus.OK).body(stockDto);
    }

    @Override
    @PutMapping("/{productCode}")
    public ResponseEntity<ResponseDto> updateStock(
            @PathVariable String productCode,
            @RequestBody StockDto stockDto) {

        boolean isUpdated = stockService.updateStock(productCode, stockDto);

        if (isUpdated) {
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(new ResponseDto(StockConstants.STATUS_200, StockConstants.MESSAGE_200));
        }

        return ResponseEntity
                .status(HttpStatus.EXPECTATION_FAILED)
                .body(new ResponseDto(StockConstants.STATUS_417, StockConstants.MESSAGE_417_UPDATE));
    }
}
