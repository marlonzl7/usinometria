package com.usinometria.api.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends BusinessException {
    public ResourceNotFoundException() {
        super("Recurso não encontrado", HttpStatus.NOT_FOUND.value(), "RESOURCE_NOT_FOUND", null);
    }
}
