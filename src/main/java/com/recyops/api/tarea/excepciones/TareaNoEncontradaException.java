package com.recyops.api.tarea.excepciones;

import com.recyops.api.comun.excepciones.RecursoNoEncontradoException;
import java.util.UUID;

public class TareaNoEncontradaException extends RecursoNoEncontradoException {

    public TareaNoEncontradaException(UUID id) {
        super("No existe la tarea con id " + id);
    }
}
