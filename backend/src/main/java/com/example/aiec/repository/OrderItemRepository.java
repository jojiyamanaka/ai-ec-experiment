package com.example.aiec.repository;

import com.example.aiec.entity.ActorType;
import com.example.aiec.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * 注文アイテムリポジトリ
 */
@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    @Modifying
    @Query("UPDATE OrderItem i SET i.isDeleted = TRUE, i.deletedAt = CURRENT_TIMESTAMP, i.deletedByType = :deletedByType, i.deletedById = :deletedById WHERE i.id = :id")
    void softDelete(@Param("id") Long id, @Param("deletedByType") ActorType deletedByType, @Param("deletedById") Long deletedById);

}
