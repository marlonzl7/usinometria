package com.usinometria.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record SetCompleteJobRequest(

        @NotNull(message = "é obrigatório")
        @DecimalMin(value = "0.0", inclusive = false, message = "deve ser maior que zero")
        @Digits(integer = 5, fraction = 2, message = "deve ter no máximo 5 dígitos inteiros e 2 casas decimais")
        BigDecimal actualTime

) {}
