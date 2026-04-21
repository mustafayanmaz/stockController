package com.musyan.stok.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(name = "Stock", description = "DTO for stock information")
public class StockDto {

    @NotNull(message = "Quantity must not be null")
    @Min(value = 0, message = "Quantity cannot be negative")
    @Schema(description = "Available stock quantity", example = "200")
    private Integer quantity;

    @NotBlank(message = "Unit must not be blank")
    @Size(max = 30, message = "Unit must not exceed 30 characters")
    @Schema(description = "Stock unit", example = "pcs")
    private String unit;
}
