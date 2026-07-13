package com.recyops.api.auth.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** POST /api/auth/recuperar — envia el correo de recuperacion de contrasena. */
public record CuerpoRecuperar(@NotBlank @Email String email) {
}
