package com.usinometria.api.exception;

import org.springframework.http.HttpStatus;

public class EstimateOutOfRangeException extends BusinessException {
    public EstimateOutOfRangeException() {
        super("A estimativa excede o limite suportado. Verifique o volume informado", HttpStatus.UNPROCESSABLE_CONTENT.value(), "ESTIMATE_OUT_OF_RANGE", null);
    }
}
