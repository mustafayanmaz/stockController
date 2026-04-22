package com.musyan.stok.repository;

import com.musyan.stok.entity.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    Optional<Product> findByProductCode(String productCode);

    boolean existsByProductCode(String productCode);

    Optional<Product> findByProductName(String productName);

    boolean existsByProductName(String productName);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.productCode = :productCode")
    Optional<Product> findByProductCodeWithLock(@Param("productCode") String productCode);

    @Modifying
    @Query("UPDATE Product p SET p.unitCost = :unitCost WHERE p.productCode = :productCode")
    void updateUnitCost(@Param("productCode") String productCode, @Param("unitCost") BigDecimal unitCost);
}