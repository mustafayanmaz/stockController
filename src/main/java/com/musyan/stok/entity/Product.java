package com.musyan.stok.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "products")
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long productId;

    @Column(nullable = false, unique = true, length = 50)
    private String productCode;

    @Column(nullable = false, unique = true, length = 150)
    private String productName;

    @Column(nullable = false, length = 100)
    private String category;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal unitCost;

    @Column(nullable = false)
    private Boolean active;

    @OneToOne(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @ToString.Exclude
    private Stock stock;
}


//request header da token gelsin validse geçişe izin ver,token control, kullanıcıadı ve passw, admin admin token yapısı
//servisin çalışabilmesi için token geçerliliği (max 1 saat gibi) (mekanizma bul token uretimi icin)