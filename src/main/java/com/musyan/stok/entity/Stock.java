package com.musyan.stok.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "stocks")
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Stock extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long stockId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false, unique = true)
    @ToString.Exclude
    private Product product;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, length = 30)
    private String unit;

    @Column(nullable = false)
    private Integer minimumStockLevel;
}
