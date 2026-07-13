package com.recyops.api.inventario.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

/** PUT /api/inventario/{id} — actualiza los topes de la linea. */
public record CuerpoTopes(
        @NotNull @PositiveOrZero BigDecimal stockMinimo,
        @NotNull @PositiveOrZero BigDecimal stockMaximo) {
}
