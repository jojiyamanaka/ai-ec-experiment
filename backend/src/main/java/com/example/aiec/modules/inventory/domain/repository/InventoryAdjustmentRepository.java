package com.example.aiec.modules.inventory.domain.repository;

import com.example.aiec.modules.inventory.domain.entity.InventoryAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryAdjustmentRepository extends JpaRepository<InventoryAdjustment, Long> {
    List<InventoryAdjustment> findByProductIdOrderByAdjustedAtDesc(Long productId);
    List<InventoryAdjustment> findAllByOrderByAdjustedAtDesc();
}
