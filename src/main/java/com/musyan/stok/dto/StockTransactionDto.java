package com.musyan.stok.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(name = "StockTransaction", description = "DTO for adding new stock and tracking its cost")
public class StockTransactionDto {

    @NotNull(message = "Quantity must not be null")
    @Min(value = 1, message = "Added quantity must be at least 1")
    @Schema(description = "Quantity to add", example = "50")
    private Integer quantity;

    @NotNull(message = "Unit cost must not be null")
    @DecimalMin(value = "0.0", message = "Unit cost must be non-negative")
    @Schema(description = "Cost price per unit", example = "100.00")
    private BigDecimal unitCost;
}
