package com.recyops.api.convenio.dtos;

import com.recyops.api.convenio.entity.Convenio;
import com.recyops.api.convenio.enums.EstadoConvenio;
import com.recyops.api.convenio.enums.TipoConvenio;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record RespuestaConvenio(
        UUID id,
        String codigo,
        String nombre,
        TipoConvenio tipo,
        UUID proveedorId,
        String proveedorNombre,
        UUID bodegaId,
        String bodegaNombre,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        BigDecimal valorTotal,
        String responsable,
        String descripcion,
        EstadoConvenio estado,
        LocalDateTime fechaCreacion) {

    public static RespuestaConvenio desde(Convenio convenio) {
        return new RespuestaConvenio(
                convenio.getId(),
                convenio.getCodigo(),
                convenio.getNombre(),
                convenio.getTipo(),
                convenio.getProveedor() != null ? convenio.getProveedor().getId() : null,
                convenio.getProveedor() != null ? convenio.getProveedor().getNombre() : null,
                convenio.getBodega() != null ? convenio.getBodega().getId() : null,
                convenio.getBodega() != null ? convenio.getBodega().getNombre() : null,
                convenio.getFechaInicio(),
                convenio.getFechaFin(),
                convenio.getValorTotal(),
                convenio.getResponsable(),
                convenio.getDescripcion(),
                convenio.getEstado(),
                convenio.getFechaCreacion());
    }
}
