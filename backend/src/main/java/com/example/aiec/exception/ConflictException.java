package com.example.aiec.exception;

import lombok.Getter;

/**
 * 競合例外（409 Conflict）
 */
@Getter
public class ConflictException extends RuntimeException {

    private final String errorCode;
    private final String errorMessage;

    public ConflictException(String errorCode, String errorMessage) {
        super(errorMessage);
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

}
