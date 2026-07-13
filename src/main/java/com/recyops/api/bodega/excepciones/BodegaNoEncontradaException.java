package com.recyops.api.bodega.excepciones;

import com.recyops.api.comun.excepciones.RecursoNoEncontradoException;
import java.util.UUID;

public class BodegaNoEncontradaException extends RecursoNoEncontradoException {

    public BodegaNoEncontradaException(UUID id) {
        super("No existe la bodega con id " + id);
    }
}
