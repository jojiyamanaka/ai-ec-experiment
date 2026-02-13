package com.example.aiec.repository;

import com.example.aiec.entity.InventoryAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryAdjustmentRepository extends JpaRepository<InventoryAdjustment, Long> {
    List<InventoryAdjustment> findByProductIdOrderByAdjustedAtDesc(Long productId);
    List<InventoryAdjustment> findAllByOrderByAdjustedAtDesc();
}
