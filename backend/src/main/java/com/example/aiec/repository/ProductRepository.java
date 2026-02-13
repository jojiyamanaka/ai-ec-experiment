package com.example.aiec.repository;

import com.example.aiec.entity.ActorType;
import com.example.aiec.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;

/**
 * 商品リポジトリ
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * 公開されている商品のみを取得
     */
    List<Product> findByIsPublishedTrue();

    /**
     * 公開されている商品のみをページネーションで取得
     */
    Page<Product> findByIsPublishedTrue(Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Product p SET p.isDeleted = TRUE, p.deletedAt = CURRENT_TIMESTAMP, p.deletedByType = :deletedByType, p.deletedById = :deletedById WHERE p.id = :id")
    void softDelete(@Param("id") Long id, @Param("deletedByType") ActorType deletedByType, @Param("deletedById") Long deletedById);

}
