package com.usinometria.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateJobRequest(

        @NotBlank(message = "é obrigatório")
        @Size(max = 150, message = "deve ter no máximo 150 caracteres")
        String name,

        @NotNull(message = "é obrigatório")
        @DecimalMin(value = "0.0", inclusive = false, message = "deve ser maior que zero")
        @Digits(integer = 10, fraction = 2, message = "deve ter no máximo 10 dígitos inteiros e 2 casas decimais")
        BigDecimal volume

) {}
