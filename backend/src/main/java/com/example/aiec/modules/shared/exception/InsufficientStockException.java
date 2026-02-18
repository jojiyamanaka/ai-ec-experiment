package com.example.aiec.modules.shared.exception;

import com.example.aiec.modules.inventory.adapter.dto.StockShortageDetail;
import lombok.Getter;

import java.util.List;

/**
 * 在庫不足例外（詳細情報付き）
 * OUT_OF_STOCKエラーで、どの商品がどれだけ不足しているかの詳細を含む
 */
@Getter
public class InsufficientStockException extends ConflictException {

    /**
     * 在庫不足商品の詳細リスト
     */
    private final List<StockShortageDetail> details;

    /**
     * コンストラクタ
     *
     * @param errorCode エラーコード
     * @param errorMessage エラーメッセージ
     * @param details 在庫不足商品の詳細リスト
     */
    public InsufficientStockException(String errorCode, String errorMessage, List<StockShortageDetail> details) {
        super(errorCode, errorMessage);
        this.details = details;
    }

}
