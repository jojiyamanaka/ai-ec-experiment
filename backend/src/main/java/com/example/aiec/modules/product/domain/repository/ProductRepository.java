package com.example.aiec.modules.product.domain.repository;

import com.example.aiec.modules.shared.domain.model.ActorType;
import com.example.aiec.modules.product.domain.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import jakarta.persistence.LockModeType;

/**
 * 商品リポジトリ
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    @Query(
            value = """
            SELECT p FROM Product p
            JOIN ProductCategory c ON p.categoryId = c.id
            WHERE p.isPublished = TRUE
              AND c.isPublished = TRUE
              AND (p.publishStartAt IS NULL OR p.publishStartAt <= :now)
              AND (p.publishEndAt IS NULL OR p.publishEndAt >= :now)
            ORDER BY p.id ASC
            """,
            countQuery = """
            SELECT COUNT(p) FROM Product p
            JOIN ProductCategory c ON p.categoryId = c.id
            WHERE p.isPublished = TRUE
              AND c.isPublished = TRUE
              AND (p.publishStartAt IS NULL OR p.publishStartAt <= :now)
              AND (p.publishEndAt IS NULL OR p.publishEndAt >= :now)
            """
    )
    Page<Product> findPublishedForCustomer(@Param("now") Instant now, Pageable pageable);

    @Query("""
            SELECT p FROM Product p
            JOIN ProductCategory c ON p.categoryId = c.id
            WHERE p.id = :id
              AND p.isPublished = TRUE
              AND c.isPublished = TRUE
              AND (p.publishStartAt IS NULL OR p.publishStartAt <= :now)
              AND (p.publishEndAt IS NULL OR p.publishEndAt >= :now)
            """)
    Optional<Product> findVisibleById(@Param("id") Long id, @Param("now") Instant now);

    @Query("""
            SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM Product p
            JOIN ProductCategory c ON p.categoryId = c.id
            WHERE p.id = :id
              AND p.isPublished = TRUE
              AND c.isPublished = TRUE
              AND (p.publishStartAt IS NULL OR p.publishStartAt <= :now)
              AND (p.publishEndAt IS NULL OR p.publishEndAt >= :now)
              AND (p.saleStartAt IS NULL OR p.saleStartAt <= :now)
              AND (p.saleEndAt IS NULL OR p.saleEndAt >= :now)
            """)
    boolean isPurchasableById(@Param("id") Long id, @Param("now") Instant now);

    @Query("""
            SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM Product p
            JOIN ProductCategory c ON p.categoryId = c.id
            WHERE p.id = :id
              AND p.isPublished = TRUE
              AND c.isPublished = TRUE
              AND (p.publishStartAt IS NULL OR p.publishStartAt <= :now)
              AND (p.publishEndAt IS NULL OR p.publishEndAt >= :now)
            """)
    boolean isVisibleById(@Param("id") Long id, @Param("now") Instant now);

    boolean existsByProductCode(String productCode);

    boolean existsByProductCodeAndIdNot(String productCode, Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Product p SET p.isDeleted = TRUE, p.deletedAt = CURRENT_TIMESTAMP, p.deletedByType = :deletedByType, p.deletedById = :deletedById WHERE p.id = :id")
    void softDelete(@Param("id") Long id, @Param("deletedByType") ActorType deletedByType, @Param("deletedById") Long deletedById);

}
