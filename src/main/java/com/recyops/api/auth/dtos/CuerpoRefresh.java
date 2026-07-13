package com.recyops.api.auth.dtos;

import jakarta.validation.constraints.NotBlank;

/** POST /api/auth/refresh — renueva la sesion con el refresh token vigente. */
public record CuerpoRefresh(@NotBlank String refreshToken) {
}
