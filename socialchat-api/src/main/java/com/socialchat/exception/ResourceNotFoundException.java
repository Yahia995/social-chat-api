package com.socialchat.exception;

public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(String resource, Long id) {
        super("RESOURCE_NOT_FOUND", resource + " not found with id: " + id);
    }

    public ResourceNotFoundException(String resource, String identifier) {
        super("RESOURCE_NOT_FOUND", resource + " not found: " + identifier);
    }
}
