package com.musyan.stok.controller;

import com.musyan.stok.dto.ResponseDto;
import com.musyan.stok.dto.StockDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Stock REST APIs", description = "REST APIs to fetch and update stock information")
public interface IStockController {

    @Operation(summary = "Fetch stock by product code", description = "REST API to fetch stock details using product code")
    ResponseEntity<StockDto> fetchStock(@NotBlank(message = "Product code must not be blank") String productCode);

    @Operation(summary = "Update stock", description = "REST API to update stock quantity, unit and minimum level by product code")
    ResponseEntity<ResponseDto> updateStock(
            @NotBlank(message = "Product code must not be blank") String productCode,
            @Valid @RequestBody StockDto stockDto);
}
