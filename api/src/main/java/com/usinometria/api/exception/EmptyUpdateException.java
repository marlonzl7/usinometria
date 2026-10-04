package com.usinometria.api.exception;

import org.springframework.http.HttpStatus;

public class EmptyUpdateException extends BusinessException {
    public EmptyUpdateException() {
        super("Informe ao menos um campo para atualizar", HttpStatus.BAD_REQUEST.value(), "EMPTY_UPDATE_REQUEST", null);
    }
}
