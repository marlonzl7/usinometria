package com.usinometria.api.dto;

import com.usinometria.api.enums.Status;

import java.math.BigDecimal;
import java.time.Instant;

public record CreateJobResponse(
        Long id,
        String name,
        BigDecimal volume,
        BigDecimal estimatedTime,
        BigDecimal actualTime,
        Status status,
        BigDecimal rateUsed,
        Instant createdAt,
        Instant finishedAt,
        Instant canceledAt,
        Instant editedAt
) {}
