package com.recyops.api.ingreso.dtos;

import com.recyops.api.ingreso.entity.DetalleIngreso;
import java.math.BigDecimal;
import java.util.UUID;

public record RespuestaDetalleIngreso(
        UUID id,
        UUID materialId,
        String categoria,
        BigDecimal pesoBruto,
        BigDecimal tara,
        BigDecimal pesoNeto,
        BigDecimal precioKilo,
        BigDecimal subtotal,
        String observaciones) {

    public static RespuestaDetalleIngreso desde(DetalleIngreso detalle) {
        return new RespuestaDetalleIngreso(
                detalle.getId(),
                detalle.getMaterial() != null ? detalle.getMaterial().getId() : null,
                detalle.getCategoria(),
                detalle.getPesoBruto(),
                detalle.getTara(),
                detalle.getPesoNeto(),
                detalle.getPrecioKilo(),
                detalle.getSubtotal(),
                detalle.getObservaciones());
    }
}
