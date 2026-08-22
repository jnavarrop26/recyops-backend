package com.recyops.api.entrega.dtos;

import com.recyops.api.entrega.entity.Entrega;
import com.recyops.api.entrega.enums.EstadoEntrega;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Forma de entrega que consume el cliente RecyOps. */
public record RespuestaEntrega(
        UUID id,
        String codigo,
        UUID convenioId,
        String convenioNombre,
        UUID bodegaId,
        String bodegaNombre,
        UUID personaEntregaId,
        String personaEntregaNombre,
        String personaEntregaCedula,
        BigDecimal totalKg,
        EstadoEntrega estado,
        LocalDateTime fechaRecepcion,
        String usuarioRegistroNombre,
        List<RespuestaLineaEntrega> lineas) {

    /** Version liviana para listados: sin las lineas de material. */
    public static RespuestaEntrega desde(Entrega entrega) {
        return construir(entrega, List.of());
    }

    /** Version completa para el detalle y el recibo impreso. */
    public static RespuestaEntrega conLineas(Entrega entrega) {
        return construir(entrega, entrega.getLineas().stream()
                .map(RespuestaLineaEntrega::desde)
                .toList());
    }

    private static RespuestaEntrega construir(Entrega entrega, List<RespuestaLineaEntrega> lineas) {
        return new RespuestaEntrega(
                entrega.getId(),
                entrega.getCodigo(),
                entrega.getConvenio() != null ? entrega.getConvenio().getId() : null,
                entrega.getConvenio() != null ? entrega.getConvenio().getNombre() : null,
                entrega.getBodega().getId(),
                entrega.getBodega().getNombre(),
                entrega.getPersonaEntrega() != null ? entrega.getPersonaEntrega().getId() : null,
                entrega.getPersonaEntrega() != null ? entrega.getPersonaEntrega().getNombreCompleto() : null,
                entrega.getPersonaEntrega() != null ? entrega.getPersonaEntrega().getCedula() : null,
                entrega.getTotalKg(),
                entrega.getEstado(),
                entrega.getFechaRecepcion(),
                entrega.getUsuarioRegistroNombre(),
                lineas);
    }
}
