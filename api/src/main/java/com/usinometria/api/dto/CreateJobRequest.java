package com.usinometria.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateJobRequest(

        @NotBlank(message = "é obrigatório")
        @Size(max = 150, message = "deve ter no máximo 150 caracteres")
        @Schema(description = "Nome/identificação da peça", example = "Eixo pinhão")
        String name,

        @NotNull(message = "é obrigatório")
        @DecimalMin(value = "0.0", inclusive = false, message = "deve ser maior que zero")
        @Digits(integer = 10, fraction = 2, message = "deve ter no máximo 10 dígitos inteiros e 2 casas decimais")
        @Schema(description = "Volume a remover, em mm³", example = "1500.00")
        BigDecimal volume

) {}
