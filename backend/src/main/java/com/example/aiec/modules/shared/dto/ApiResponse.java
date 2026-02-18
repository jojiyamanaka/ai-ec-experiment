package com.example.aiec.modules.shared.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * API共通レスポンス形式
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "API共通レスポンス")
public class ApiResponse<T> {

    @Schema(description = "成功フラグ", example = "true")
    private boolean success;
    @Schema(description = "レスポンスデータ")
    private T data;
    @Schema(description = "エラー詳細（エラー時のみ）")
    private ErrorDetail error;

    /**
     * 成功レスポンスを生成
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }

    /**
     * エラーレスポンスを生成
     */
    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(false, null, new ErrorDetail(code, message, null));
    }

    /**
     * 詳細情報付きエラーレスポンスを生成
     */
    public static <T> ApiResponse<T> errorWithDetails(String code, String message, Object details) {
        return new ApiResponse<>(false, null, new ErrorDetail(code, message, details));
    }

    /**
     * エラー詳細
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "エラー詳細")
    public static class ErrorDetail {
        @Schema(description = "エラーコード", example = "NOT_FOUND")
        private String code;
        @Schema(description = "エラーメッセージ", example = "リソースが見つかりません")
        private String message;
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @Schema(description = "追加のエラー情報")
        private Object details;
    }

}
