package com.example.aiec.repository;

import com.example.aiec.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 注文アイテムリポジトリ
 */
@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

}
