package com.recyops.api.usuario.dtos;

import com.recyops.api.usuario.entity.Usuario;
import java.util.UUID;

public record RespuestaTrabajadorCreado(
        UUID id,
        String username,
        String email,
        String estado,
        String passwordTemporal) {

    public static RespuestaTrabajadorCreado desde(Usuario usuario, String passwordTemporal) {
        return new RespuestaTrabajadorCreado(
                usuario.getId(),
                usuario.getUsername(),
                usuario.getEmail(),
                usuario.getEstado().name(),
                passwordTemporal);
    }
}
