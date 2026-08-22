package com.recyops.api.entrega.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** POST /api/entregas — si fechaRecepcion es null se usa la fecha actual. */
public record CuerpoEntrega(
        @NotNull UUID convenioId,
        @NotNull UUID bodegaId,
        @NotNull UUID personaEntregaId,
        LocalDateTime fechaRecepcion,
        @NotEmpty List<@Valid CuerpoLineaEntrega> lineas) {
}
