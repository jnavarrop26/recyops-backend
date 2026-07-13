package com.recyops.api.material.dtos;

import com.recyops.api.material.enums.UnidadEmpaque;
import com.recyops.api.material.enums.UnidadMedida;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

/** POST y PUT /api/materiales — los clasificadores llegan por codigo. */
public record CuerpoMaterial(
        @NotBlank String nombre,
        @NotBlank String categoriaCodigo,
        String subcategoriaCodigo,
        String codigoResinaCodigo,
        String colorCodigo,
        @NotNull UnidadMedida unidadMedida,
        @NotNull UnidadEmpaque unidadEmpaque,
        @NotNull @Positive BigDecimal precioBase,
        @NotNull @Positive BigDecimal factorCalidad,
        BigDecimal umbralMerma) {
}
