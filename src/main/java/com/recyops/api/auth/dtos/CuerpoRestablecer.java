package com.recyops.api.auth.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * POST /api/auth/restablecer — fija la nueva contrasena usando el token
 * de recuperacion que llega en el enlace del correo.
 */
public record CuerpoRestablecer(
        @NotBlank String accessToken,
        @NotBlank @Size(min = 6, message = "La contrasena debe tener al menos 6 caracteres") String password) {
}
