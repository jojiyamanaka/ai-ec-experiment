package com.example.aiec.modules.inventory.application.port;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 在庫不足商品の詳細情報
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockShortageDetail {

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 商品名
     */
    private String productName;

    /**
     * 要求数量
     */
    private Integer requestedQuantity;

    /**
     * 利用可能在庫数
     */
    private Integer availableStock;

}
