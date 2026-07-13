package com.recyops.api.tarea.dtos;

import com.recyops.api.tarea.entity.Tarea;
import com.recyops.api.tarea.enums.EstadoTarea;
import com.recyops.api.tarea.enums.PrioridadTarea;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record RespuestaTarea(
        UUID id,
        String titulo,
        String descripcion,
        UUID asignadoId,
        String asignadoNombre,
        UUID bodegaId,
        String bodegaNombre,
        PrioridadTarea prioridad,
        EstadoTarea estado,
        LocalDate fechaLimite,
        boolean vencida,
        String creadoPorNombre,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaCompletada) {

    public static RespuestaTarea desde(Tarea tarea) {
        boolean vencida = tarea.getFechaLimite() != null
                && tarea.getFechaLimite().isBefore(LocalDate.now())
                && tarea.getEstado() != EstadoTarea.COMPLETADA
                && tarea.getEstado() != EstadoTarea.CANCELADA;
        return new RespuestaTarea(
                tarea.getId(),
                tarea.getTitulo(),
                tarea.getDescripcion(),
                tarea.getAsignado().getId(),
                tarea.getAsignado().getNombreCompleto(),
                tarea.getBodega() != null ? tarea.getBodega().getId() : null,
                tarea.getBodega() != null ? tarea.getBodega().getNombre() : null,
                tarea.getPrioridad(),
                tarea.getEstado(),
                tarea.getFechaLimite(),
                vencida,
                tarea.getCreadoPorNombre(),
                tarea.getFechaCreacion(),
                tarea.getFechaCompletada());
    }
}
