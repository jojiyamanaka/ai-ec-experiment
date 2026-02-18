package com.example.aiec.modules.inventory.adapter.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InventoryStatusDto {
    private Long productId;
    private String productName;
    private Integer physicalStock;
    private Integer tentativeReserved;
    private Integer committedReserved;
    private Integer availableStock;
}
