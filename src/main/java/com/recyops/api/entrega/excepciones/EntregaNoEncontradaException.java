package com.recyops.api.entrega.excepciones;

import com.recyops.api.comun.excepciones.RecursoNoEncontradoException;
import java.util.UUID;

public class EntregaNoEncontradaException extends RecursoNoEncontradoException {

    public EntregaNoEncontradaException(UUID id) {
        super("No existe la entrega con id " + id);
    }
}
