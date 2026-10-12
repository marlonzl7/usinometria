package com.usinometria.api.dto;

import com.usinometria.api.enums.Status;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

public record CreateJobResponse(
        @Schema(description = "Identificador do trabalho", example = "25") Long id,
        @Schema(description = "Nome/identificação da peça", example = "Eixo pinhão") String name,
        @Schema(description = "Volume a remover, em mm³", example = "1500.00") BigDecimal volume,
        @Schema(description = "Tempo estimado, em horas (decimal), calculado na criação", example = "15.58") BigDecimal estimatedTime,
        @Schema(description = "Tempo real, em horas (decimal); nulo enquanto IN_PROGRESS", example = "14.70", nullable = true) BigDecimal actualTime,
        Status status,
        @Schema(description = "Taxa global usada no cálculo, em mm³/h") BigDecimal rateUsed,
        @Schema(description = "Criação (UTC)") Instant createdAt,
        @Schema(description = "Preenchido ao concluir", nullable = true) Instant finishedAt,
        @Schema(description = "Preenchido ao cancelar", nullable = true) Instant canceledAt,
        @Schema(description = "Preenchido apenas quando um concluído é editado", nullable = true) Instant editedAt
) {}
