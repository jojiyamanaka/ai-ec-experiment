package com.example.aiec.modules.purchase.shipment.repository;

import com.example.aiec.modules.purchase.shipment.entity.ShipmentItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShipmentItemRepository extends JpaRepository<ShipmentItem, Long> {
}
