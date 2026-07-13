package com.recyops.api.convenio.dtos;

import com.recyops.api.convenio.enums.TipoConvenio;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** POST y PUT /api/convenios. */
public record CuerpoConvenio(
        @NotBlank String nombre,
        @NotNull TipoConvenio tipo,
        UUID proveedorId,
        UUID bodegaId,
        @NotNull LocalDate fechaInicio,
        LocalDate fechaFin,
        BigDecimal valorTotal,
        String responsable,
        String descripcion) {
}
