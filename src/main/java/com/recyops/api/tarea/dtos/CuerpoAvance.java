package com.recyops.api.tarea.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** POST /api/tareas/{id}/avances — una entrada de la bitacora. */
public record CuerpoAvance(
        @Positive BigDecimal cantidad,
        @NotBlank @Size(max = 300) String descripcion) {
}
