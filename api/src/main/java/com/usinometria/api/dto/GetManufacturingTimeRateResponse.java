package com.usinometria.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

public record GetManufacturingTimeRateResponse(
        @Schema(description = "Taxa global ΣVolume/ΣTempoReal, em mm³/h; nulo quando não há histórico",
                example = "96.25000", nullable = true)
        BigDecimal rate,

        @Schema(description = "Quantidade de trabalhos COMPLETED que sustentam a taxa")
        Integer quantity
) {}
