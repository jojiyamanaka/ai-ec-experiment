package com.example.aiec.dto;

import com.example.aiec.entity.StockReservation;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 引当レスポンスDTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservationDto {

    private Long reservationId;
    private Long productId;
    private Integer quantity;
    private String type;
    private Instant expiresAt;
    private Integer availableStock;

    public static ReservationDto fromEntity(StockReservation reservation, Integer availableStock) {
        ReservationDto dto = new ReservationDto();
        dto.setReservationId(reservation.getId());
        dto.setProductId(reservation.getProduct().getId());
        dto.setQuantity(reservation.getQuantity());
        dto.setType(reservation.getType().name());
        dto.setExpiresAt(reservation.getExpiresAt());
        dto.setAvailableStock(availableStock);
        return dto;
    }

}
