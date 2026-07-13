package com.recyops.api.auth.dtos;

import jakarta.validation.constraints.NotBlank;

/** POST /api/auth/login — el username es el correo registrado en Supabase Auth. */
public record CuerpoLogin(
        @NotBlank String username,
        @NotBlank String password) {
}
