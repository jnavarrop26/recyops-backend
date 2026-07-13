package com.recyops.api.material.dtos;

import jakarta.validation.constraints.NotNull;

/** PATCH /api/materiales/{id} — activa o desactiva el material. */
public record CuerpoCambioActivo(@NotNull Boolean activo) {
}
