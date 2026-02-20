package com.example.aiec.modules.purchase.order.repository;

import com.example.aiec.modules.shared.domain.model.ActorType;
import com.example.aiec.modules.product.domain.entity.AllocationType;
import com.example.aiec.modules.purchase.order.entity.Order;
import com.example.aiec.modules.purchase.order.entity.OrderItem;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 注文アイテムリポジトリ
 */
@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    @Modifying
    @Query("UPDATE OrderItem i SET i.isDeleted = TRUE, i.deletedAt = CURRENT_TIMESTAMP, i.deletedByType = :deletedByType, i.deletedById = :deletedById WHERE i.id = :id")
    void softDelete(@Param("id") Long id, @Param("deletedByType") ActorType deletedByType, @Param("deletedById") Long deletedById);

    @Query("""
            SELECT COALESCE(SUM(oi.quantity), 0)
            FROM OrderItem oi
            JOIN oi.order o
            JOIN oi.product p
            WHERE p.id = :productId
              AND p.allocationType = :allocationType
              AND o.status <> :cancelledStatus
            """)
    Integer sumOrderedQuantityByProductAndAllocationType(@Param("productId") Long productId,
                                                         @Param("allocationType") AllocationType allocationType,
                                                         @Param("cancelledStatus") Order.OrderStatus cancelledStatus);

    @Query("""
            SELECT COALESCE(SUM(oi.quantity), 0)
            FROM OrderItem oi
            JOIN oi.order o
            JOIN oi.product p
            WHERE p.id = :productId
              AND p.allocationType = :allocationType
              AND o.status <> :cancelledStatus
              AND o.id <> :excludedOrderId
            """)
    Integer sumOrderedQuantityByProductAndAllocationTypeExcludingOrder(@Param("productId") Long productId,
                                                                        @Param("allocationType") AllocationType allocationType,
                                                                        @Param("cancelledStatus") Order.OrderStatus cancelledStatus,
                                                                        @Param("excludedOrderId") Long excludedOrderId);

    @Query("""
            SELECT COALESCE(SUM(oi.committedQty), 0)
            FROM OrderItem oi
            JOIN oi.order o
            WHERE oi.product.id = :productId
              AND o.status <> :cancelledStatus
            """)
    Integer sumCommittedQuantityByProductExcludingCancelled(@Param("productId") Long productId,
                                                            @Param("cancelledStatus") Order.OrderStatus cancelledStatus);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT oi
            FROM OrderItem oi
            JOIN oi.order o
            JOIN oi.product p
            WHERE p.id = :productId
              AND p.allocationType = :allocationType
              AND o.status IN :statuses
              AND oi.committedQty < oi.quantity
            ORDER BY o.createdAt ASC, o.id ASC, oi.id ASC
            """)
    List<OrderItem> findPendingItemsForAllocation(@Param("productId") Long productId,
                                                  @Param("allocationType") AllocationType allocationType,
                                                  @Param("statuses") List<Order.OrderStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT oi
            FROM OrderItem oi
            JOIN oi.order o
            JOIN oi.product p
            WHERE o.id = :orderId
              AND p.allocationType = :allocationType
              AND o.status IN :statuses
              AND oi.committedQty < oi.quantity
            ORDER BY o.createdAt ASC, o.id ASC, oi.id ASC
            """)
    List<OrderItem> findPendingItemsForAllocationByOrderId(@Param("orderId") Long orderId,
                                                            @Param("allocationType") AllocationType allocationType,
                                                            @Param("statuses") List<Order.OrderStatus> statuses);

}
