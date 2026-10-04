package com.usinometria.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdateCompleteJobRequest(

        @Pattern(regexp = "(?s).*\\S.*", message = "não pode ser vazio")
        @Size(max = 150, message = "deve ter no máximo 150 caracteres")
        String name,

        @DecimalMin(value = "0.0", inclusive = false, message = "deve ser maior que zero")
        @Digits(integer = 10, fraction = 2, message = "deve ter no máximo 10 dígitos inteiros e 2 casas decimais")
        BigDecimal volume,

        @DecimalMin(value = "0.0", inclusive = false, message = "deve ser maior que zero")
        @Digits(integer = 5, fraction = 2, message = "deve ter no máximo 5 dígitos inteiros e 2 casas decimais")
        BigDecimal actualTime

) {}
