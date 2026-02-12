package com.example.aiec.exception;

import com.example.aiec.dto.UnavailableProductDetail;
import lombok.Getter;

import java.util.List;

/**
 * 非公開商品例外（詳細情報付き）
 */
@Getter
public class ItemNotAvailableException extends BusinessException {

    private final List<UnavailableProductDetail> details;

    public ItemNotAvailableException(String errorCode, String errorMessage, List<UnavailableProductDetail> details) {
        super(errorCode, errorMessage);
        this.details = details;
    }

}
