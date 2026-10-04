package com.usinometria.api.dto;

import com.usinometria.api.enums.Status;

import java.math.BigDecimal;
import java.time.Instant;

public record SetCompleteJobResponse(
        Long id,
        String name,
        BigDecimal volume,
        BigDecimal estimatedTime,
        BigDecimal actualTime,
        Status status,
        BigDecimal difference,
        Instant createdAt,
        Instant finishedAt,
        Instant canceledAt,
        Instant editedAt
) {}
