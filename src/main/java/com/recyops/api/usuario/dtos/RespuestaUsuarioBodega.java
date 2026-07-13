package com.recyops.api.usuario.dtos;

import com.recyops.api.usuario.entity.Usuario;
import java.util.UUID;

/** Usuario asignado a una bodega (GET /api/bodegas/{id}/usuarios). */
public record RespuestaUsuarioBodega(
        UUID id,
        String nombreCompleto,
        String username,
        String rol,
        String estado) {

    public static RespuestaUsuarioBodega desde(Usuario usuario) {
        return new RespuestaUsuarioBodega(
                usuario.getId(),
                usuario.getNombreCompleto(),
                usuario.getUsername(),
                usuario.getRol().getNombre(),
                usuario.getEstado().name());
    }
}
