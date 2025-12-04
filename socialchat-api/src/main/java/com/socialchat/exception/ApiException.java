package com.socialchat.exception;

import lombok.Getter;

@Getter
public class ApiException extends RuntimeException {

    private final String code;
    private final Object details;

    public ApiException(String code, String message) {
        super(message);
        this.code = code;
        this.details = null;
    }

    public ApiException(String code, String message, Object details) {
        super(message);
        this.code = code;
        this.details = details;
    }
}
