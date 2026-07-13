package com.recyops.api.comun.excepciones;

import org.springframework.http.HttpStatus;

public class ReglaNegocioException extends ExcepcionRecyOps {

    public ReglaNegocioException(String mensaje) {
        super(HttpStatus.BAD_REQUEST, mensaje);
    }
}
