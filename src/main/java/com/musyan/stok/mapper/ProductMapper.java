package com.musyan.stok.mapper;

import com.musyan.stok.dto.ProductDto;
import com.musyan.stok.entity.Product;

public final class ProductMapper {

    private ProductMapper() {
    }

    public static ProductDto mapToProductDto(Product product, ProductDto productDto) {
        productDto.setProductId(product.getProductId());
        productDto.setProductCode(product.getProductCode());
        productDto.setProductName(product.getProductName());
        productDto.setCategory(product.getCategory());
        productDto.setUnitCost(product.getUnitCost());
        productDto.setActive(product.getActive());
        productDto.setQuantity(product.getQuantity());
        productDto.setMinStockLevel(product.getMinStockLevel());
        productDto.setUnit(product.getUnit());
        productDto.setCreatedAt(product.getCreatedAt() != null ? product.getCreatedAt().toLocalDate() : null);
        return productDto;
    }

    public static Product mapToProduct(ProductDto productDto, Product product) {
        product.setProductCode(productDto.getProductCode());
        product.setProductName(productDto.getProductName());
        product.setCategory(productDto.getCategory());
        product.setUnitCost(productDto.getUnitCost());
        product.setActive(productDto.getActive());
        if (productDto.getUnit() != null) {
            product.setUnit(productDto.getUnit());
        }

        if (productDto.getCreatedAt() != null) {
            product.setCreatedAt(productDto.getCreatedAt().atTime(9, 0, 0));
        } else {
            product.setCreatedAt(java.time.LocalDate.now().atTime(9, 0, 0));
        }
        return product;
    }
}