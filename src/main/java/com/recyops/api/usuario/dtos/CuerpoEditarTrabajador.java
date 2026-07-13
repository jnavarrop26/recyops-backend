package com.recyops.api.usuario.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * PUT /api/admin/usuarios/{id} — edicion de un trabajador.
 * El email y el username son la identidad del usuario y no se editan.
 */
public record CuerpoEditarTrabajador(
        @NotBlank String nombreCompleto,
        String telefono,
        @NotNull UUID bodegaId,
        @NotNull UUID rolId) {
}
