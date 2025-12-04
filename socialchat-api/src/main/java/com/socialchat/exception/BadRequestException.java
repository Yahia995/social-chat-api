package com.socialchat.exception;

public class BadRequestException extends ApiException {

    public BadRequestException(String message) {
        super("BAD_REQUEST", message);
    }

    public BadRequestException(String code, String message) {
        super(code, message);
    }
}
