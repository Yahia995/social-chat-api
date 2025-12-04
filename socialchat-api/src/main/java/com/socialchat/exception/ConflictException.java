package com.socialchat.exception;

public class ConflictException extends ApiException {

    public ConflictException(String message) {
        super("CONFLICT", message);
    }

    public ConflictException(String code, String message) {
        super(code, message);
    }
}
