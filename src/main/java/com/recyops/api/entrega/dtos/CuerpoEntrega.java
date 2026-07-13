package com.recyops.api.entrega.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** POST /api/entregas — si fechaRecepcion es null se usa la fecha actual. */
public record CuerpoEntrega(
        @NotNull UUID proveedorId,
        @NotNull UUID bodegaId,
        @NotNull UUID tipoMaterialId,
        @NotNull @Positive BigDecimal pesoKg,
        String personaEntrega,
        LocalDateTime fechaRecepcion) {
}
