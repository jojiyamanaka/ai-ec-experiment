package com.example.aiec.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 有効在庫レスポンスDTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AvailabilityDto {

    private Long productId;
    private Integer physicalStock;
    private Integer tentativeReserved;
    private Integer committedReserved;
    private Integer availableStock;

}
