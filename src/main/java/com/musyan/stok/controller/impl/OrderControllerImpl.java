package com.musyan.stok.controller.impl;

import com.musyan.stok.controller.IOrderController;
import com.musyan.stok.dto.OrderRequestDto;
import com.musyan.stok.dto.OrderResponseDto;
import com.musyan.stok.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Validated
public class OrderControllerImpl implements IOrderController {

    private final OrderService orderService;

    @Override
    public ResponseEntity<OrderResponseDto> createOrder(@RequestBody OrderRequestDto orderRequestDto) {
        OrderResponseDto response = orderService.createOrder(orderRequestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
