package com.musyan.stok.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@Schema(name = "OrderResponse", description = "DTO returned after sale order creation")
public class OrderResponseDto {

    private Long orderId;
    private String orderCode;
    private String productCode;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalAmount;
    private BigDecimal totalCost;
}
