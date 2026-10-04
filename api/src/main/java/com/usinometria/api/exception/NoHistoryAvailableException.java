package com.usinometria.api.exception;

import org.springframework.http.HttpStatus;

public class NoHistoryAvailableException extends BusinessException {
    public NoHistoryAvailableException() {
        super("Não há histórico disponível para gerar uma estimativa", HttpStatus.UNPROCESSABLE_CONTENT.value(), "NO_HISTORY_AVAILABLE", null);
    }
}
