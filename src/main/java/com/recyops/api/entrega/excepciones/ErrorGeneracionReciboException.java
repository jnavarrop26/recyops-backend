package com.recyops.api.entrega.excepciones;

import com.recyops.api.comun.excepciones.ExcepcionRecyOps;
import org.springframework.http.HttpStatus;

/** Fallo tecnico armando el PDF del recibo de una entrega. */
public class ErrorGeneracionReciboException extends ExcepcionRecyOps {

    public ErrorGeneracionReciboException(String codigoEntrega, Throwable causa) {
        super(HttpStatus.INTERNAL_SERVER_ERROR,
                "No fue posible generar el recibo de la entrega " + codigoEntrega);
        initCause(causa);
    }
}
