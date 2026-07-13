package com.recyops.api.ingreso.excepciones;

import com.recyops.api.comun.excepciones.RecursoNoEncontradoException;
import java.util.UUID;

public class IngresoNoEncontradoException extends RecursoNoEncontradoException {

    public IngresoNoEncontradoException(Long id) {
        super("No existe el ingreso con id " + id);
    }

    public IngresoNoEncontradoException(UUID uuid) {
        super("No existe el ingreso con codigo " + uuid);
    }
}
