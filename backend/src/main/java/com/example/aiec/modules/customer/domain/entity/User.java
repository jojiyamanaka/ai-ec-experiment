package com.example.aiec.modules.customer.domain.entity;

import com.example.aiec.modules.shared.domain.model.ActorType;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "users")
@SQLDelete(sql = "UPDATE users SET is_deleted = TRUE, deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@Where(clause = "is_deleted = FALSE")
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "terms_agreed_at")
    private Instant termsAgreedAt;

    @Column(name = "full_name", length = 100)
    private String fullName;

    @Column(name = "phone_number", length = 30)
    private String phoneNumber;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "newsletter_opt_in", nullable = false)
    private Boolean newsletterOptIn = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "member_rank", nullable = false, length = 50)
    private MemberRank memberRank = MemberRank.STANDARD;

    @Column(name = "loyalty_points", nullable = false)
    private Integer loyaltyPoints = 0;

    @Column(name = "deactivation_reason", length = 500)
    private String deactivationReason;

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

    public enum MemberRank {
        STANDARD,
        SILVER,
        GOLD,
        PLATINUM
    }
}
