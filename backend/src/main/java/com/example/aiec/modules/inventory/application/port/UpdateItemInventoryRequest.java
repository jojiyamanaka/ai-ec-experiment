package com.example.aiec.modules.inventory.application.port;

import com.example.aiec.modules.product.domain.entity.AllocationType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 管理向け在庫タブ更新リクエスト
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateItemInventoryRequest {

    private AllocationType allocationType;
    private LocationStockInput locationStock;
    private SalesLimitInput salesLimit;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LocationStockInput {
        private Integer allocatableQty;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SalesLimitInput {
        private Integer salesLimitTotal;
    }
}
