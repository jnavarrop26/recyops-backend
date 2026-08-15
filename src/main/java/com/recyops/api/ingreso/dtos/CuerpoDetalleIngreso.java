package com.recyops.api.ingreso.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Un material dentro del cuerpo de POST /api/ingresos.
 * La categoria y el precio base se resuelven desde el catalogo (el precio
 * puede venir explicito si la operacion lo ajusta en bascula).
 */
public record CuerpoDetalleIngreso(
        @NotNull UUID materialId,
        @NotNull @Positive BigDecimal pesoBruto,
        @NotNull @PositiveOrZero BigDecimal tara,
        @PositiveOrZero BigDecimal precioKilo,
        String observaciones) {
}
