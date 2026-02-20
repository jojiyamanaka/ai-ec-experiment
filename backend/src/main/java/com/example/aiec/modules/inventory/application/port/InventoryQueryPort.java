package com.example.aiec.modules.inventory.application.port;

import com.example.aiec.modules.inventory.application.port.AvailabilityDto;
import com.example.aiec.modules.inventory.application.port.InventoryStatusDto;

import java.util.List;

/**
 * 在庫クエリAPI（公開インターフェース）
 */
public interface InventoryQueryPort {

    /**
     * 有効在庫を取得する
     */
    AvailabilityDto getAvailableStock(Long productId);

    /**
     * 全商品の在庫状況を一括取得
     */
    List<InventoryStatusDto> getAllInventoryStatus();

    /**
     * 管理向け在庫タブ情報を取得
     */
    AdminItemInventoryDto getAdminItemInventory(Long productId);

    /**
     * 商品の有効在庫を算出
     */
    Integer calculateEffectiveStock(Long productId);

}
