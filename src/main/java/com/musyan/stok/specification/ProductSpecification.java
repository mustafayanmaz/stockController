package com.musyan.stok.specification;

import com.musyan.stok.dto.ProductFilterDto;
import com.musyan.stok.entity.Product;
import com.musyan.stok.entity.Stock;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

//order islemi yapılacak birer birer satın alıncak
//hangi stok kartından alındıgını takip et satıs üzerinde stok numarası olamalı
//fifo  , ortalama maliyet hesabını method olarak yap, aktif stoktaki urunlerimin tutarı uzerinden yapılması lazım
//kendi zamanındaki fiyatına gore olacak ortalama maliyet arttı zam yapılmalı tarzı bir mantık çıkmalı ortaya
// ortalama maliyet hesabını service içinde değilde asenkron olarak çağırma işlemi olsun

public final class ProductSpecification {

    private ProductSpecification() {
    }

    public static Specification<Product> buildFilter(ProductFilterDto filter) {
        return (Root<Product> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter.getProductCode() != null && !filter.getProductCode().isBlank()) {
                predicates.add(cb.like(
                        cb.lower(root.get("productCode")),
                        "%" + filter.getProductCode().toLowerCase() + "%"));
            }

            if (filter.getProductName() != null && !filter.getProductName().isBlank()) {
                predicates.add(cb.like(
                        cb.lower(root.get("productName")),
                        "%" + filter.getProductName().toLowerCase() + "%"));
            }

            if (filter.getCategory() != null && !filter.getCategory().isBlank()) {
                predicates.add(cb.equal(root.get("category"), filter.getCategory()));
            }

            if (filter.getMinPrice() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("unitCost"), filter.getMinPrice()));
            }

            if (filter.getMaxPrice() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("unitCost"), filter.getMaxPrice()));
            }

            if (filter.getActive() != null) {
                predicates.add(cb.equal(root.get("active"), filter.getActive()));
            }

            if (Boolean.TRUE.equals(filter.getInStock())) {
                Join<Product, Stock> stockJoin = root.join("stock", JoinType.INNER);
                predicates.add(cb.greaterThan(stockJoin.get("quantity"), 0));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
