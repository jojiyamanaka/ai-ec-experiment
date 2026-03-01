package com.example.aiec.modules.purchase.application.port;

public interface ReturnCommandPort {

    ReturnShipmentDto createReturn(Long orderId, Long userId, CreateReturnRequest request);

    ReturnShipmentDto approveReturn(Long orderId);

    ReturnShipmentDto rejectReturn(Long orderId, RejectReturnRequest request);

    ReturnShipmentDto confirmReturn(Long orderId);
}
