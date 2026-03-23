package com.musyan.stok.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(name = "ProductFilter", description = "DTO for filtering products with dynamic criteria")
public class ProductFilterDto {

    @Schema(description = "Filter by product code (partial match)", example = "PRD")
    private String productCode;

    @Schema(description = "Filter by product name (partial match)", example = "Laptop")
    private String productName;

    @Schema(description = "Filter by category (exact match)", example = "Electronics")
    private String category;

    @Schema(description = "Minimum unit cost", example = "100.00")
    private BigDecimal minPrice;

    @Schema(description = "Maximum unit cost", example = "5000.00")
    private BigDecimal maxPrice;

    @Schema(description = "Filter by active status", example = "true")
    private Boolean active;

    @Schema(description = "Filter only products that have stock (quantity > 0)", example = "true")
    private Boolean inStock;
}
