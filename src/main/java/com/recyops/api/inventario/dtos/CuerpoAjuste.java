package com.recyops.api.inventario.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

/** POST /api/inventario/{id}/ajuste — fija el stock en una cantidad nueva. */
public record CuerpoAjuste(
        @NotNull @PositiveOrZero BigDecimal cantidadNueva,
        @NotBlank String motivo) {
}
