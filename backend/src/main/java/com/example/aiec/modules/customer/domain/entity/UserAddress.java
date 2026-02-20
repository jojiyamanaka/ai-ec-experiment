package com.example.aiec.modules.customer.domain.entity;

import com.example.aiec.modules.shared.domain.model.ActorType;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.Instant;

@Entity
@Table(name = "user_addresses")
@SQLDelete(sql = "UPDATE user_addresses SET is_deleted = TRUE, deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@Where(clause = "is_deleted = FALSE")
@Data
public class UserAddress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(length = 100)
    private String label;

    @Column(name = "recipient_name", nullable = false, length = 100)
    private String recipientName;

    @Column(name = "recipient_phone_number", length = 30)
    private String recipientPhoneNumber;

    @Column(name = "postal_code", nullable = false, length = 20)
    private String postalCode;

    @Column(nullable = false, length = 100)
    private String prefecture;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(name = "address_line1", nullable = false, length = 255)
    private String addressLine1;

    @Column(name = "address_line2", length = 255)
    private String addressLine2;

    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;

    @Column(name = "address_order", nullable = false)
    private Integer addressOrder = 0;

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
