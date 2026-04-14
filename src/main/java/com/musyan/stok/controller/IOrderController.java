package com.musyan.stok.controller;

import com.musyan.stok.dto.OrderRequestDto;
import com.musyan.stok.dto.OrderResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Order REST APIs", description = "REST APIs for sales order operations")
@RequestMapping(path = "/api/orders")
public interface IOrderController {

    @Operation(summary = "Create sale order", description = "REST API to create a sale order with FIFO stock consumption")
    @PostMapping
    ResponseEntity<OrderResponseDto> createOrder(@Valid @RequestBody OrderRequestDto orderRequestDto);
}
