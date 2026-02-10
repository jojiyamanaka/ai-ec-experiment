package com.example.aiec.exception;

import lombok.Getter;

/**
 * ビジネスロジック例外
 */
@Getter
public class BusinessException extends RuntimeException {

    private final String errorCode;
    private final String errorMessage;

    public BusinessException(String errorCode, String errorMessage) {
        super(errorMessage);
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

}
