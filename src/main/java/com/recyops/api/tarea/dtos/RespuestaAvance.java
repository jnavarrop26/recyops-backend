package com.recyops.api.tarea.dtos;

import com.recyops.api.tarea.entity.AvanceTarea;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record RespuestaAvance(
        UUID id,
        BigDecimal cantidad,
        String descripcion,
        String usuarioNombre,
        LocalDateTime fechaRegistro) {

    public static RespuestaAvance desde(AvanceTarea avance) {
        return new RespuestaAvance(
                avance.getId(),
                avance.getCantidad(),
                avance.getDescripcion(),
                avance.getUsuarioNombre(),
                avance.getFechaRegistro());
    }
}
