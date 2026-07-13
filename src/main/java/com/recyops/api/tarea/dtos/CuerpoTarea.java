package com.recyops.api.tarea.dtos;

import com.recyops.api.tarea.enums.PrioridadTarea;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

/** POST y PUT /api/tareas — creacion/edicion por el administrador. */
public record CuerpoTarea(
        @NotBlank String titulo,
        String descripcion,
        @NotNull UUID asignadoId,
        UUID bodegaId,
        @NotNull PrioridadTarea prioridad,
        LocalDate fechaLimite) {
}
