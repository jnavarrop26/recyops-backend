package com.recyops.api.inventario.dtos;

import com.recyops.api.inventario.entity.MovimientoInventario;
import com.recyops.api.inventario.enums.TipoOperacion;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record RespuestaMovimiento(
        UUID id,
        TipoOperacion tipoOperacion,
        BigDecimal cantidad,
        BigDecimal cantidadAnterior,
        BigDecimal cantidadNueva,
        String referencia,
        String usuarioNombre,
        LocalDateTime fechaRegistro) {

    public static RespuestaMovimiento desde(MovimientoInventario movimiento) {
        return new RespuestaMovimiento(
                movimiento.getId(),
                movimiento.getTipoOperacion(),
                movimiento.getCantidad(),
                movimiento.getCantidadAnterior(),
                movimiento.getCantidadNueva(),
                movimiento.getReferencia(),
                movimiento.getUsuarioNombre(),
                movimiento.getFechaRegistro());
    }
}
