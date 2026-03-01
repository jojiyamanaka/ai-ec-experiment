package com.example.aiec.modules.purchase.shipment.repository;

import com.example.aiec.modules.purchase.shipment.entity.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, Long> {

    boolean existsByOrderIdAndShipmentType(Long orderId, Shipment.ShipmentType shipmentType);

    List<Shipment> findByStatusOrderByCreatedAtAsc(Shipment.ShipmentStatus status);

    boolean existsByExportFilePathAndStatus(String exportFilePath, Shipment.ShipmentStatus status);

    Optional<Shipment> findByOrderIdAndShipmentType(Long orderId, Shipment.ShipmentType shipmentType);

    Page<Shipment> findByShipmentTypeOrderByCreatedAtDesc(Shipment.ShipmentType shipmentType, Pageable pageable);

    Page<Shipment> findByShipmentTypeAndStatusOrderByCreatedAtDesc(
            Shipment.ShipmentType shipmentType,
            Shipment.ShipmentStatus status,
            Pageable pageable
    );
}
