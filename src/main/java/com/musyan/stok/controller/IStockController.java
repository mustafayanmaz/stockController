package com.musyan.stok.controller;

import com.musyan.stok.dto.ResponseDto;
import com.musyan.stok.dto.StockDto;
import com.musyan.stok.dto.StockRemoveDto;
import com.musyan.stok.dto.StockTransactionDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Stock REST APIs", description = "REST APIs to fetch and update stock information")
@RequestMapping(path = "/api/stocks")
public interface IStockController {

    @Operation(summary = "Fetch stock by product code", description = "REST API to fetch stock details using product code")
    @GetMapping("/{productCode}")
    ResponseEntity<StockDto> fetchStock(@PathVariable("productCode") @NotBlank(message = "Product code must not be blank") String productCode);

    @Operation(summary = "Update stock", description = "REST API to update stock quantity, unit and minimum level by product code")
    @PutMapping("/{productCode}")
    ResponseEntity<ResponseDto> updateStock(
            @PathVariable("productCode") @NotBlank(message = "Product code must not be blank") String productCode,
            @Valid @RequestBody StockDto stockDto);

    @Operation(summary = "Add stock transaction", description = "REST API to add a new stock entry recalculating average cost")
    @PostMapping("/{productCode}/add")
    ResponseEntity<ResponseDto> addStockTransaction(
            @PathVariable("productCode") @NotBlank(message = "Product code must not be blank") String productCode,
            @Valid @RequestBody StockTransactionDto transactionDto);

    @Operation(summary = "Remove stock", description = "REST API to remove (consume) quantity from existing stock")
    @PostMapping("/{productCode}/remove")
    ResponseEntity<ResponseDto> removeStockTransaction(
            @PathVariable("productCode") @NotBlank(message = "Product code must not be blank") String productCode,
            @Valid @RequestBody StockRemoveDto removeDto);
}
