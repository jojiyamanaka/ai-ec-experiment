package com.example.aiec.modules.inventory.domain.entity;

import com.example.aiec.modules.product.domain.entity.Product;
import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;

@Entity
@Table(name = "inventory_adjustments")
@Data
public class InventoryAdjustment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Integer quantityBefore;

    @Column(nullable = false)
    private Integer quantityAfter;

    @Column(nullable = false)
    private Integer quantityDelta; // 増減量（例: +10, -5）

    @Column(nullable = false, length = 500)
    private String reason;

    @Column(nullable = false, length = 255)
    private String adjustedBy; // メールアドレス

    @Column(nullable = false)
    private Instant adjustedAt;
}
