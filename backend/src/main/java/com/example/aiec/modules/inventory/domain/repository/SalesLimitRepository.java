package com.example.aiec.modules.inventory.domain.repository;

import com.example.aiec.modules.inventory.domain.entity.SalesLimit;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 枠在庫リポジトリ
 */
@Repository
public interface SalesLimitRepository extends JpaRepository<SalesLimit, Long> {

    Optional<SalesLimit> findByProductId(Long productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT sl FROM SalesLimit sl WHERE sl.product.id = :productId")
    Optional<SalesLimit> findByProductIdForUpdate(@Param("productId") Long productId);
}
