package com.example.aiec.modules.inventory.application.port;

import com.example.aiec.modules.product.domain.entity.AllocationType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 管理向け商品在庫タブDTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminItemInventoryDto {

    private Long productId;
    private AllocationType allocationType;
    private LocationStockDto locationStock;
    private SalesLimitDto salesLimit;
}
