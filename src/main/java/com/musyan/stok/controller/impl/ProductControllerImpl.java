package com.musyan.stok.controller.impl;

import com.musyan.stok.constants.StockConstants;
import com.musyan.stok.controller.IProductController;
import com.musyan.stok.dto.ProductDto;
import com.musyan.stok.dto.ProductFilterDto;
import com.musyan.stok.dto.ResponseDto;
import com.musyan.stok.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Validated
public class ProductControllerImpl implements IProductController {

    private final ProductService productService;

    @Override
    public ResponseEntity<ResponseDto> createProduct(@RequestBody ProductDto productDto) {
        productService.createProduct(productDto);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new ResponseDto(StockConstants.STATUS_201, StockConstants.MESSAGE_201));
    }

    @Override
    public ResponseEntity<ProductDto> fetchProduct(@PathVariable String productCode) {
        ProductDto productDto = productService.fetchProductByCode(productCode);
        return ResponseEntity.status(HttpStatus.OK).body(productDto);
    }

    @Override
    public ResponseEntity<List<ProductDto>> fetchAllProducts() {
        return ResponseEntity.ok(productService.fetchAllProducts());
    }

    @Override
    public ResponseEntity<List<ProductDto>> filterProducts(@RequestBody ProductFilterDto filterDto) {
        return ResponseEntity.ok(productService.filterProducts(filterDto));
    }

    @Override
    public ResponseEntity<ResponseDto> updateProduct(@RequestBody ProductDto productDto) {
        boolean isUpdated = productService.updateProduct(productDto);

        if (isUpdated) {
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(new ResponseDto(StockConstants.STATUS_200, StockConstants.MESSAGE_200));
        }

        return ResponseEntity
                .status(HttpStatus.EXPECTATION_FAILED)
                .body(new ResponseDto(StockConstants.STATUS_417, StockConstants.MESSAGE_417_UPDATE));
    }

    @Override
    public ResponseEntity<ResponseDto> deleteProduct(@PathVariable String productCode) {
        boolean isDeleted = productService.deleteProductByCode(productCode);

        if (isDeleted) {
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(new ResponseDto(StockConstants.STATUS_200, StockConstants.MESSAGE_200));
        }

        return ResponseEntity
                .status(HttpStatus.EXPECTATION_FAILED)
                .body(new ResponseDto(StockConstants.STATUS_417, StockConstants.MESSAGE_417_DELETE));
    }

    @Override
    public ResponseEntity<Boolean> validateProductCode(@PathVariable String productCode) {
        return ResponseEntity.ok(productService.existsByProductCode(productCode));
    }
}
