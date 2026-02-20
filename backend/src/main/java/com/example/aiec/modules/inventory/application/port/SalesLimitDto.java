package com.example.aiec.modules.inventory.application.port;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 枠在庫DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SalesLimitDto {

    private Integer frameLimitQty;
    private Integer consumedQty;
    private Integer remainingQty;
}
