package com.recyops.api.usuario.excepciones;

import com.recyops.api.comun.excepciones.RecursoNoEncontradoException;
import java.util.UUID;

public class UsuarioNoEncontradoException extends RecursoNoEncontradoException {

    public UsuarioNoEncontradoException(UUID id) {
        super("No existe el trabajador con id " + id);
    }
}
