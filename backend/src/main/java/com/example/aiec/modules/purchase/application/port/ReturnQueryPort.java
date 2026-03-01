package com.example.aiec.modules.purchase.application.port;

import com.example.aiec.modules.purchase.shipment.entity.Shipment;

public interface ReturnQueryPort {

    ReturnShipmentDto getReturnByOrderId(Long orderId, Long userId);

    ReturnListResponse getAllReturns(Shipment.ShipmentStatus status, int page, int limit);
}
