package com.recyops.api.inventario.excepciones;

import com.recyops.api.comun.excepciones.ReglaNegocioException;

public class StockInvalidoException extends ReglaNegocioException {

    public StockInvalidoException(String mensaje) {
        super(mensaje);
    }
}
