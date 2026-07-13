package com.recyops.api.usuario.dtos;

import com.recyops.api.usuario.entity.Rol;
import java.util.UUID;

public record RespuestaRol(UUID id, String nombre) {

    public static RespuestaRol desde(Rol rol) {
        return new RespuestaRol(rol.getId(), rol.getNombre());
    }
}
