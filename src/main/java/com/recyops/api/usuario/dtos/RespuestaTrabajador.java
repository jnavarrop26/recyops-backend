package com.recyops.api.usuario.dtos;

import com.recyops.api.usuario.entity.Usuario;
import java.util.UUID;

public record RespuestaTrabajador(
        UUID id,
        String nombreCompleto,
        String username,
        String email,
        String telefono,
        String estado,
        UUID rolId,
        String rolNombre,
        UUID bodegaId,
        String bodegaNombre) {

    public static RespuestaTrabajador desde(Usuario usuario) {
        return new RespuestaTrabajador(
                usuario.getId(),
                usuario.getNombreCompleto(),
                usuario.getUsername(),
                usuario.getEmail(),
                usuario.getTelefono(),
                usuario.getEstado().name(),
                usuario.getRol().getId(),
                usuario.getRol().getNombre(),
                usuario.getBodega() != null ? usuario.getBodega().getId() : null,
                usuario.getBodega() != null ? usuario.getBodega().getNombre() : null);
    }
}
