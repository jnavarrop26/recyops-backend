package com.recyops.api.ingreso.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.List;

/**
 * POST /api/ingresos — registro de un ingreso de material en bascula.
 * Si llega la lista de materiales, el backend recalcula los totales desde ella.
 */
public record CuerpoIngreso(
        @NotBlank String cliente,
        @NotBlank String cedula,
        @NotBlank String bodegaDestino,
        @NotBlank String encargado,
        String placaVehiculo,
        @NotNull @Positive BigDecimal pesoNetoTotal,
        @NotNull @Positive BigDecimal total,
        @Valid List<CuerpoDetalleIngreso> materiales) {
}
