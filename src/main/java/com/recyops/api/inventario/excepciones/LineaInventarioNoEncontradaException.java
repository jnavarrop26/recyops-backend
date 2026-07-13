package com.recyops.api.inventario.excepciones;

import com.recyops.api.comun.excepciones.RecursoNoEncontradoException;
import java.util.UUID;

public class LineaInventarioNoEncontradaException extends RecursoNoEncontradoException {

    public LineaInventarioNoEncontradaException(UUID id) {
        super("No existe la linea de inventario con id " + id);
    }
}
