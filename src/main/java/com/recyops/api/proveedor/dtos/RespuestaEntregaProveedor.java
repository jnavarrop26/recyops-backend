package com.recyops.api.proveedor.dtos;

import com.recyops.api.entrega.entity.Entrega;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Entrega resumida para el detalle del proveedor (GET /api/proveedores/{id}/entregas). */
public record RespuestaEntregaProveedor(
        UUID id,
        String codigo,
        String tipoMaterialNombre,
        BigDecimal pesoKg,
        String estado,
        LocalDateTime fechaRecepcion) {

    public static RespuestaEntregaProveedor desde(Entrega entrega) {
        return new RespuestaEntregaProveedor(
                entrega.getId(),
                entrega.getCodigo(),
                entrega.getTipoMaterial().getNombre(),
                entrega.getPesoKg(),
                entrega.getEstado().name(),
                entrega.getFechaRecepcion());
    }
}
