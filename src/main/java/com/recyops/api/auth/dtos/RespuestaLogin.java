package com.recyops.api.auth.dtos;

/** Respuesta del login y del refresh: token de acceso + refresh token rotado. */
public record RespuestaLogin(
        String token,
        String refreshToken,
        String rol,
        String nombreCompleto,
        String username) {
}
