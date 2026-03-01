package com.example.aiec.modules.product.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import com.example.aiec.modules.shared.domain.model.ActorType;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * 商品エンティティ
 */
@Entity
@Table(name = "products")
@SQLDelete(sql = "UPDATE products SET is_deleted = TRUE, deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@Where(clause = "is_deleted = FALSE")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal price;

    @Column(nullable = false, length = 500)
    private String image;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false)
    private Integer stock;

    @Enumerated(EnumType.STRING)
    @Column(name = "allocation_type", nullable = false, length = 20)
    private AllocationType allocationType = AllocationType.REAL;

    @Column(name = "is_published", nullable = false)
    private Boolean isPublished = true;

    @Column(name = "is_returnable", nullable = false)
    private Boolean isReturnable = true;

    @Column(name = "product_code", nullable = false, unique = true, length = 100)
    private String productCode;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(name = "publish_start_at")
    private Instant publishStartAt;

    @Column(name = "publish_end_at")
    private Instant publishEndAt;

    @Column(name = "sale_start_at")
    private Instant saleStartAt;

    @Column(name = "sale_end_at")
    private Instant saleEndAt;

    // 監査カラム
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "created_by_type", length = 50)
    private ActorType createdByType;

    @Column(name = "created_by_id")
    private Long createdById;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "updated_by_type", length = 50)
    private ActorType updatedByType;

    @Column(name = "updated_by_id")
    private Long updatedById;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "deleted_by_type", length = 50)
    private ActorType deletedByType;

    @Column(name = "deleted_by_id")
    private Long deletedById;

    public Product(Long id, String name, Integer price, String image, String description, Integer stock, Boolean isPublished) {
        this.id = id;
        this.name = name;
        this.price = price != null ? BigDecimal.valueOf(price.longValue()) : null;
        this.image = image;
        this.description = description;
        this.stock = stock;
        this.allocationType = AllocationType.REAL;
        this.isPublished = isPublished;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
        isDeleted = false;
        if (createdByType == null) {
            createdByType = ActorType.SYSTEM;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

}
