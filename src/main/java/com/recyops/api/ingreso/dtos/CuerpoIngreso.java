package com.recyops.api.ingreso.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * POST /api/ingresos — registro de un ingreso de material en bascula.
 * Los totales se recalculan siempre desde los materiales, que ademas
 * alimentan el inventario de la bodega destino (un ENTRADA por material).
 */
public record CuerpoIngreso(
        @NotBlank String cliente,
        @NotBlank String cedula,
        @NotNull UUID bodegaDestinoId,
        @NotBlank String encargado,
        String placaVehiculo,
        @NotNull @Positive BigDecimal pesoNetoTotal,
        @NotNull @Positive BigDecimal total,
        @NotEmpty List<@Valid CuerpoDetalleIngreso> materiales) {
}
