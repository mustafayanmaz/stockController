package com.musyan.stok.controller;

import com.musyan.stok.dto.ProductDto;
import com.musyan.stok.dto.ProductFilterDto;
import com.musyan.stok.dto.ResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Tag(name = "Product REST APIs", description = "REST APIs to create, read, update, delete and filter products")
public interface IProductController {

    @Operation(summary = "Create product", description = "REST API to create a new product with stock information")
    ResponseEntity<ResponseDto> createProduct(@Valid @RequestBody ProductDto productDto);

    @Operation(summary = "Fetch product by code", description = "REST API to fetch product details using product code")
    ResponseEntity<ProductDto> fetchProduct(@NotBlank(message = "Product code must not be blank") String productCode);

    @Operation(summary = "Fetch all products", description = "REST API to fetch all products with their stock information")
    ResponseEntity<List<ProductDto>> fetchAllProducts();

    @Operation(summary = "Filter products", description = "REST API to filter products by criteria (name, category, price range, stock availability)")
    ResponseEntity<List<ProductDto>> filterProducts(@RequestBody ProductFilterDto filterDto);

    @Operation(summary = "Update product", description = "REST API to update product details")
    ResponseEntity<ResponseDto> updateProduct(@Valid @RequestBody ProductDto productDto);

    @Operation(summary = "Delete product", description = "REST API to delete a product by product code")
    ResponseEntity<ResponseDto> deleteProduct(@NotBlank(message = "Product code must not be blank") String productCode);

    @Operation(summary = "Validate product code", description = "REST API to check whether a product code exists")
    ResponseEntity<Boolean> validateProductCode(
            @NotBlank(message = "Product code must not be blank") String productCode);
}
