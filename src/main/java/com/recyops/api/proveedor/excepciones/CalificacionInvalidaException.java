package com.recyops.api.proveedor.excepciones;

import com.recyops.api.comun.excepciones.ReglaNegocioException;

public class CalificacionInvalidaException extends ReglaNegocioException {

    public CalificacionInvalidaException(double valor) {
        super("La calificacion debe estar entre 0 y 5; se recibio " + valor);
    }
}
