package com.musyan.stok.service;

import com.musyan.stok.dto.ProductDto;
import com.musyan.stok.dto.ProductFilterDto;
import com.musyan.stok.entity.Product;
import com.musyan.stok.entity.Stock;
import com.musyan.stok.exception.ProductAlreadyExistsException;
import com.musyan.stok.exception.ResourceNotFoundException;
import com.musyan.stok.mapper.ProductMapper;
import com.musyan.stok.mapper.StockMapper;
import com.musyan.stok.repository.ProductRepository;
import com.musyan.stok.specification.ProductSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional
    public void createProduct(ProductDto productDto) {
        if (productRepository.existsByProductCode(productDto.getProductCode())) {
            throw new ProductAlreadyExistsException(
                    "Product already exists with code: " + productDto.getProductCode());
        }

        if (productRepository.existsByProductName(productDto.getProductName())) {
            throw new ProductAlreadyExistsException(
                    "Product already exists with name: " + productDto.getProductName());
        }

        Product product = ProductMapper.mapToProduct(productDto, new Product());

        if (productDto.getStock() != null) {
            Stock stock = StockMapper.mapToStock(productDto.getStock(), new Stock());
            stock.setProduct(product);
            product.setStock(stock);
        }

        productRepository.save(product);
    }

    public ProductDto fetchProductByCode(String productCode) {
        Product product = productRepository.findByProductCode(productCode)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productCode", productCode));

        return ProductMapper.mapToProductDto(product, new ProductDto());
    }

    public List<ProductDto> fetchAllProducts() {
        return productRepository.findAll()
                .stream()
                .map(product -> ProductMapper.mapToProductDto(product, new ProductDto()))
                .toList();
    }

    public List<ProductDto> filterProducts(ProductFilterDto filterDto) {
        return productRepository.findAll(ProductSpecification.buildFilter(filterDto))
                .stream()
                .map(product -> ProductMapper.mapToProductDto(product, new ProductDto()))
                .toList();
    }

    @Transactional
    public boolean updateProduct(ProductDto productDto) {
        Product existingProduct = productRepository.findByProductCode(productDto.getProductCode())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product", "productCode", productDto.getProductCode()));

        if (!existingProduct.getProductName().equals(productDto.getProductName()) &&
                productRepository.existsByProductName(productDto.getProductName())) {
            throw new ProductAlreadyExistsException(
                    "Product already exists with name: " + productDto.getProductName());
        }

        ProductMapper.mapToProduct(productDto, existingProduct);

        if (productDto.getStock() != null && existingProduct.getStock() != null) {
            StockMapper.mapToStock(productDto.getStock(), existingProduct.getStock());
        }

        productRepository.save(existingProduct);
        return true;
    }

    @Transactional
    public boolean deleteProductByCode(String productCode) {
        Product product = productRepository.findByProductCode(productCode)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productCode", productCode));

        productRepository.delete(product);
        return true;
    }

    public boolean existsByProductCode(String productCode) {
        return productRepository.existsByProductCode(productCode);
    }
}
