package com.recyops.api.usuario.excepciones;

import com.recyops.api.comun.excepciones.RecursoNoEncontradoException;
import java.util.UUID;

public class RolNoEncontradoException extends RecursoNoEncontradoException {

    public RolNoEncontradoException(UUID id) {
        super("No existe el rol con id " + id);
    }
}
