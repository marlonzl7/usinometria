package com.usinometria.api.common;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
        @Schema(description = "Código HTTP")
        int status,

        @Schema(description = "Código estável do erro, para tratamento programático",
                allowableValues = {"INVALID_PARAMETER", "VALIDATION_ERROR", "MALFORMED_REQUEST",
                        "UNSUPPORTED_MEDIA_TYPE", "NO_HISTORY_AVAILABLE", "ESTIMATE_OUT_OF_RANGE",
                        "RESOURCE_NOT_FOUND", "UNEXPECTED_STATUS_JOB", "EMPTY_UPDATE_REQUEST",
                        "METHOD_NOT_ALLOWED", "ROUTE_NOT_FOUND", "INTERNAL_SERVER_ERROR"})
        String error,

        @Schema(description = "Mensagem legível")
        String message,

        @Schema(description = "Erros por campo, só em VALIDATION_ERROR, no formato \"campo: mensagem\"")
        List<String> details,

        @Schema(description = "Momento do erro (UTC)")
        Instant timestamp
) {}
