package com.recyops.api.comun.excepciones;

import org.springframework.http.HttpStatus;

public class RecursoNoEncontradoException extends ExcepcionRecyOps {

    public RecursoNoEncontradoException(String mensaje) {
        super(HttpStatus.NOT_FOUND, mensaje);
    }
}
