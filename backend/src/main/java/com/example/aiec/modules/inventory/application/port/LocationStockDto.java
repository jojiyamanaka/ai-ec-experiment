package com.example.aiec.modules.inventory.application.port;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 拠点在庫DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LocationStockDto {

    private Integer locationId;
    private Integer availableQty;
    private Integer committedQty;
    private Integer remainingQty;
}
