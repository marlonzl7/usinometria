package com.usinometria.api.exception;

import lombok.Getter;

import java.util.List;

@Getter
public class BusinessException extends RuntimeException {

    private final int statusCode;
    private final String error;
    private final List<String> details;

    public BusinessException(String message, int statusCode, String error, List<String> details) {
        super(message);
        this.statusCode = statusCode;
        this.error = error;
        this.details = details;
    }

}
