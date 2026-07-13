package com.recyops.api.proveedor.excepciones;

import com.recyops.api.comun.excepciones.RecursoNoEncontradoException;
import java.util.UUID;

public class ProveedorNoEncontradoException extends RecursoNoEncontradoException {

    public ProveedorNoEncontradoException(UUID id) {
        super("No existe el proveedor con id " + id);
    }
}
