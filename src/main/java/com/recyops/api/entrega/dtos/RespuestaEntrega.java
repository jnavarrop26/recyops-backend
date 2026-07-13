package com.recyops.api.entrega.dtos;

import com.recyops.api.entrega.entity.Entrega;
import com.recyops.api.entrega.enums.EstadoEntrega;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Forma de entrega que consume el cliente RecyOps. */
public record RespuestaEntrega(
        UUID id,
        String codigo,
        UUID proveedorId,
        String proveedorNombre,
        UUID bodegaId,
        String bodegaNombre,
        UUID tipoMaterialId,
        String tipoMaterialNombre,
        BigDecimal pesoKg,
        String personaEntrega,
        EstadoEntrega estado,
        LocalDateTime fechaRecepcion,
        String usuarioRegistroNombre) {

    public static RespuestaEntrega desde(Entrega entrega) {
        return new RespuestaEntrega(
                entrega.getId(),
                entrega.getCodigo(),
                entrega.getProveedor().getId(),
                entrega.getProveedor().getNombre(),
                entrega.getBodega().getId(),
                entrega.getBodega().getNombre(),
                entrega.getTipoMaterial().getId(),
                entrega.getTipoMaterial().getNombre(),
                entrega.getPesoKg(),
                entrega.getPersonaEntrega(),
                entrega.getEstado(),
                entrega.getFechaRecepcion(),
                entrega.getUsuarioRegistroNombre());
    }
}
