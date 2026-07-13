package com.recyops.api.dashboard.dtos;

/** Indicadores del tablero principal (vista Home del cliente). */
public record RespuestaResumenDashboard(
        long totalBodegas,
        long bodegasActivas,
        long totalProveedores,
        long entregasHoy,
        long lineasBajoMinimo) {
}
