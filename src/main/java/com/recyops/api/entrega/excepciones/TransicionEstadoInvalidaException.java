package com.recyops.api.entrega.excepciones;

import com.recyops.api.comun.excepciones.ReglaNegocioException;
import com.recyops.api.entrega.enums.EstadoEntrega;

public class TransicionEstadoInvalidaException extends ReglaNegocioException {

    public TransicionEstadoInvalidaException(EstadoEntrega actual, EstadoEntrega solicitado) {
        super("No se puede pasar la entrega de " + actual + " a " + solicitado
                + "; el flujo es RECIBIDA > EN_PROCESO > PROCESADA > DESPACHADA");
    }
}
