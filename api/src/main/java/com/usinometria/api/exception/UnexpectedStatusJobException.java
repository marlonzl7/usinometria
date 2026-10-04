package com.usinometria.api.exception;

import org.springframework.http.HttpStatus;

public class UnexpectedStatusJobException extends BusinessException {
    public UnexpectedStatusJobException(String expected) {
        super("Trabalho com status inesperado. Esperava: " + expected, HttpStatus.CONFLICT.value(), "UNEXPECTED_STATUS_JOB", null);
    }
}
