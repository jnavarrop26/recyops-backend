package com.recyops.api.material.excepciones;

import com.recyops.api.comun.excepciones.RecursoNoEncontradoException;
import java.util.UUID;

public class MaterialNoEncontradoException extends RecursoNoEncontradoException {

    public MaterialNoEncontradoException(UUID id) {
        super("No existe el material con id " + id);
    }
}
