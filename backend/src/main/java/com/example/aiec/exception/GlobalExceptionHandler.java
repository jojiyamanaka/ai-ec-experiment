package com.example.aiec.exception;

import com.example.aiec.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * グローバル例外ハンドラー
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * リソースが見つからない例外
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFound(ResourceNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getErrorCode(), ex.getErrorMessage()));
    }

    /**
     * 非公開商品例外（詳細情報付き）
     */
    @ExceptionHandler(ItemNotAvailableException.class)
    public ResponseEntity<ApiResponse<Void>> handleItemNotAvailableException(ItemNotAvailableException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.errorWithDetails(ex.getErrorCode(), ex.getErrorMessage(), ex.getDetails()));
    }

    /**
     * 在庫不足例外（詳細情報付き）
     */
    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ApiResponse<Void>> handleInsufficientStockException(InsufficientStockException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.errorWithDetails(ex.getErrorCode(), ex.getErrorMessage(), ex.getDetails()));
    }

    /**
     * ビジネスロジック例外
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
        // 認証エラーは 401 Unauthorized
        if ("UNAUTHORIZED".equals(ex.getErrorCode())
                || "INVALID_CREDENTIALS".equals(ex.getErrorCode())
                || "INVALID_TOKEN".equals(ex.getErrorCode())
                || "TOKEN_REVOKED".equals(ex.getErrorCode())
                || "TOKEN_EXPIRED".equals(ex.getErrorCode())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(ex.getErrorCode(), ex.getErrorMessage()));
        }
        // その他のビジネス例外は 400 Bad Request
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getErrorCode(), ex.getErrorMessage()));
    }

    /**
     * 認可エラー（403 Forbidden）
     */
    @ExceptionHandler(ForbiddenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResponse<Void> handleForbiddenException(ForbiddenException ex) {
        return ApiResponse.error(ex.getCode(), ex.getMessage());
    }

    /**
     * 競合例外（409 Conflict）
     */
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiResponse<Void>> handleConflictException(ConflictException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getErrorCode(), ex.getErrorMessage()));
    }

    /**
     * バリデーション例外
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        String message = errors.values().stream()
                .findFirst()
                .orElse("無効なリクエストです");

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("INVALID_REQUEST", message));
    }

    /**
     * 必須ヘッダー不足
     */
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingRequestHeaderException(MissingRequestHeaderException ex) {
        if ("Authorization".equals(ex.getHeaderName())) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("UNAUTHORIZED", "認証が必要です"));
        }

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("INVALID_REQUEST", "必須ヘッダーが不足しています: " + ex.getHeaderName()));
    }

    /**
     * その他の例外
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception ex) {
        ex.printStackTrace();
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("INTERNAL_ERROR", "内部エラーが発生しました"));
    }

}
