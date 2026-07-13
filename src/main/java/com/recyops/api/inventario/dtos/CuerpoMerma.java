package com.recyops.api.inventario.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

/** POST /api/inventario/{id}/merma — descuenta una perdida de material. */
public record CuerpoMerma(
        @NotNull @Positive BigDecimal cantidad,
        @NotBlank String motivo) {
}
