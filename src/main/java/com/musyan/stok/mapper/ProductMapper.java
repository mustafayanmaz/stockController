package com.musyan.stok.mapper;

import com.musyan.stok.dto.ProductDto;
import com.musyan.stok.dto.StockDto;
import com.musyan.stok.entity.Product;

public final class ProductMapper {

    private ProductMapper() {
    }

    public static ProductDto mapToProductDto(Product product, ProductDto productDto) {
        productDto.setProductId(product.getProductId());
        productDto.setProductCode(product.getProductCode());
        productDto.setProductName(product.getProductName());
        productDto.setCategory(product.getCategory());
        productDto.setDescription(product.getDescription());
        productDto.setUnitCost(product.getUnitCost());
        productDto.setActive(product.getActive());

        if (product.getStock() != null) {
            StockDto stockDto = new StockDto();
            StockMapper.mapToStockDto(product.getStock(), stockDto);
            productDto.setStock(stockDto);
        }

        return productDto;
    }

    public static Product mapToProduct(ProductDto productDto, Product product) {
        product.setProductCode(productDto.getProductCode());
        product.setProductName(productDto.getProductName());
        product.setCategory(productDto.getCategory());
        product.setDescription(productDto.getDescription());
        product.setUnitCost(productDto.getUnitCost());
        product.setActive(productDto.getActive());
        return product;
    }
}