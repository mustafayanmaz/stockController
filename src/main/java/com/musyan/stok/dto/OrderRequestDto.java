package com.musyan.stok.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(name = "OrderRequest", description = "DTO for creating a sale order")
public class OrderRequestDto {

    @NotBlank(message = "Product code must not be blank")
    @Schema(description = "Product code", example = "LAPTOP-001")
    private String productCode;

    @NotNull(message = "Quantity must not be null")
    @Min(value = 1, message = "Order quantity must be at least 1")
    @Schema(description = "Quantity to sell", example = "5")
    private Integer quantity;

    @NotNull(message = "Unit price must not be null")
    @DecimalMin(value = "0.0", message = "Unit price must be non-negative")
    @Schema(description = "Selling unit price", example = "1350.00")
    private BigDecimal unitPrice;
}
