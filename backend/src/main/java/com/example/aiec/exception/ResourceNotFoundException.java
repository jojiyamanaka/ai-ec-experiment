package com.example.aiec.exception;

import lombok.Getter;

/**
 * リソースが見つからない例外
 */
@Getter
public class ResourceNotFoundException extends RuntimeException {

    private final String errorCode;
    private final String errorMessage;

    public ResourceNotFoundException(String errorCode, String errorMessage) {
        super(errorMessage);
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

}
