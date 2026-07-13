package com.recyops.api.dashboard.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Actividad de un dia para la tabla del tablero principal. */
public record RespuestaActividadDia(
        LocalDate fecha,
        long ingresos,
        BigDecimal pesoIngresadoKg,
        BigDecimal valorIngresos,
        long entregas,
        BigDecimal pesoRecibidoKg) {
}
