package com.recyops.api.material.excepciones;

import com.recyops.api.comun.excepciones.RecursoNoEncontradoException;
import com.recyops.api.material.enums.TipoOpcionCatalogo;

public class OpcionCatalogoNoEncontradaException extends RecursoNoEncontradoException {

    public OpcionCatalogoNoEncontradaException(TipoOpcionCatalogo tipo, String codigo) {
        super("No existe la opcion de catalogo " + tipo + " con codigo '" + codigo + "'");
    }
}
