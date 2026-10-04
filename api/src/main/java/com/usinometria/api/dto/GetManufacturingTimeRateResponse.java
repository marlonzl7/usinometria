package com.usinometria.api.dto;

import java.math.BigDecimal;

public record GetManufacturingTimeRateResponse(
        BigDecimal rate,
        Integer quantity
) {}
