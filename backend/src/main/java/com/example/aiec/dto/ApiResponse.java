package com.example.aiec.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API共通レスポンス形式
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    private T data;
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
    public static class ErrorDetail {
        private String code;
        private String message;
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private Object details;
    }

}
