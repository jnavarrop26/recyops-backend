package com.recyops.api.entrega.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.UUID;

/** Un material dentro del cuerpo de POST /api/entregas. */
public record CuerpoLineaEntrega(
        @NotNull UUID tipoMaterialId,
        @NotNull @Positive BigDecimal pesoKg) {
}
