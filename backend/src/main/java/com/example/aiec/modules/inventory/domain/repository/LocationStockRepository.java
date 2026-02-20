package com.example.aiec.modules.inventory.domain.repository;

import com.example.aiec.modules.inventory.domain.entity.LocationStock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 拠点在庫リポジトリ
 */
@Repository
public interface LocationStockRepository extends JpaRepository<LocationStock, Long> {

    Optional<LocationStock> findByProductIdAndLocationId(Long productId, Integer locationId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ls FROM LocationStock ls WHERE ls.product.id = :productId AND ls.locationId = :locationId")
    Optional<LocationStock> findByProductIdAndLocationIdForUpdate(@Param("productId") Long productId,
                                                                   @Param("locationId") Integer locationId);
}
