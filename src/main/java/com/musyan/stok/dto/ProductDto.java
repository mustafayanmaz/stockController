package com.musyan.stok.dto;

import com.musyan.stok.validation.ValidProductCode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(name = "Product", description = "DTO for product operations in stock microservice")
public class ProductDto {

    @Schema(description = "Product database id", example = "1")
    private Long productId;

    @NotBlank(message = "Product code must not be blank")
    @Size(min = 3, max = 50, message = "Product code must be between 3 and 50 characters")
    @ValidProductCode // KENDI CUSTOM ANOTASYONUMUZU EKLEDIK
    @Schema(description = "Unique product code", example = "PRD-1001")
    private String productCode;

    @NotBlank(message = "Product name must not be blank")
    @Size(max = 150, message = "Product name must not exceed 150 characters")
    @Schema(description = "Product name", example = "Laptop Battery")
    private String productName;

    @NotBlank(message = "Category must not be blank")
    @Size(max = 100, message = "Category must not exceed 100 characters")
    @Schema(description = "Product category", example = "Electronics")
    private String category;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    @Schema(description = "Product description", example = "Rechargeable lithium battery")
    private String description;

    @NotNull(message = "Unit cost must not be null")
    @DecimalMin(value = "0.0", inclusive = false, message = "Unit cost must be greater than 0")
    @Digits(integer = 10, fraction = 2, message = "Unit cost format is invalid")
    @Schema(description = "Cost of one unit", example = "1499.99")
    private BigDecimal unitCost;

    @NotNull(message = "Active status must not be null")
    @Schema(description = "Whether the product is active", example = "true")
    private Boolean active;

    @Valid
    @Schema(description = "Stock information for this product")
    private StockDto stock;
}