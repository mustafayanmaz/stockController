package com.musyan.stok.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(name = "StockRemove", description = "DTO for removing (consuming) stock quantity")
public class StockRemoveDto {

    @NotNull(message = "Quantity must not be null")
    @Min(value = 1, message = "Removed quantity must be at least 1")
    @Schema(description = "Quantity to remove from stock", example = "10")
    private Integer quantity;
}
