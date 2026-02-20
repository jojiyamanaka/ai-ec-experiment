package com.example.aiec.modules.product.domain.entity;

import com.example.aiec.modules.shared.domain.model.ActorType;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.Instant;

@Entity
@Table(name = "product_categories")
@SQLDelete(sql = "UPDATE product_categories SET is_deleted = TRUE, deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@Where(clause = "is_deleted = FALSE")
@Data
public class ProductCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String name;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Column(name = "is_published", nullable = false)
    private Boolean isPublished = true;

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
