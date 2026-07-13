package com.recyops.api.usuario.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/** POST /api/admin/usuarios — registro de un trabajador por el administrador. */
public record CuerpoTrabajador(
        @NotBlank String nombreCompleto,
        @NotBlank String username,
        @NotBlank @Email String email,
        String telefono,
        @NotNull UUID bodegaId,
        @NotNull UUID rolId,
        @Size(min = 8, message = "La contrasena debe tener al menos 8 caracteres") String password) {
}
