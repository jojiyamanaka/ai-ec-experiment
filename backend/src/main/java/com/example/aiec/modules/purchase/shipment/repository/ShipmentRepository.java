package com.example.aiec.modules.purchase.shipment.repository;

import com.example.aiec.modules.purchase.shipment.entity.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, Long> {

    boolean existsByOrderIdAndShipmentType(Long orderId, Shipment.ShipmentType shipmentType);

    List<Shipment> findByStatusOrderByCreatedAtAsc(Shipment.ShipmentStatus status);

    boolean existsByExportFilePathAndStatus(String exportFilePath, Shipment.ShipmentStatus status);
}
