package com.recyops.api.ingreso.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Un material dentro del cuerpo de POST /api/ingresos.
 * Con materialId, la categoria y el precio se resuelven desde el catalogo
 * (el precio puede venir explicito si la operacion lo ajusta en bascula);
 * sin materialId, la categoria es obligatoria (compatibilidad con clientes viejos).
 */
public record CuerpoDetalleIngreso(
        UUID materialId,
        String categoria,
        @NotNull @Positive BigDecimal pesoBruto,
        @NotNull @PositiveOrZero BigDecimal tara,
        @PositiveOrZero BigDecimal precioKilo,
        String observaciones) {
}
