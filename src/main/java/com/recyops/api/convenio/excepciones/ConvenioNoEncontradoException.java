package com.recyops.api.convenio.excepciones;

import com.recyops.api.comun.excepciones.RecursoNoEncontradoException;
import java.util.UUID;

public class ConvenioNoEncontradoException extends RecursoNoEncontradoException {

    public ConvenioNoEncontradoException(UUID id) {
        super("No existe el convenio con id " + id);
    }
}
