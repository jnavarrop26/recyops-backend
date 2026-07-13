package com.recyops.api.inventario.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.util.UUID;

/** POST /api/inventario — abre una linea de stock para un material en una bodega. */
public record CuerpoCrearLinea(
        @NotNull UUID bodegaId,
        @NotNull UUID tipoMaterialId,
        @NotNull @PositiveOrZero BigDecimal stockMinimo,
        @NotNull @PositiveOrZero BigDecimal stockMaximo) {
}
