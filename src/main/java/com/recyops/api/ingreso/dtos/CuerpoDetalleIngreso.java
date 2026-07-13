package com.recyops.api.ingreso.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

/** Un material dentro del cuerpo de POST /api/ingresos. */
public record CuerpoDetalleIngreso(
        @NotBlank String categoria,
        @NotNull @Positive BigDecimal pesoBruto,
        @NotNull @PositiveOrZero BigDecimal tara,
        @NotNull @PositiveOrZero BigDecimal precioKilo,
        String observaciones) {
}
