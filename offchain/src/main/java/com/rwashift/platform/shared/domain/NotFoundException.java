package com.rwashift.platform.shared.domain;

public class NotFoundException extends RuntimeException {

    public NotFoundException(String resourceType, String id) {
        super("%s '%s' was not found".formatted(resourceType, id));
    }
}
