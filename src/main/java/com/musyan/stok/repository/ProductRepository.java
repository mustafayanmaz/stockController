package com.musyan.stok.repository;

import com.musyan.stok.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    Optional<Product> findByProductCode(String productCode);

    boolean existsByProductCode(String productCode);
}