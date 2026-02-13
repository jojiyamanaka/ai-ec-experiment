package com.example.aiec.exception;

/**
 * 認可エラー（403 Forbidden）
 */
public class ForbiddenException extends RuntimeException {
    private final String code;
    private final String message;

    public ForbiddenException(String code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
