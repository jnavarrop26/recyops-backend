package com.recyops.api.comun.excepciones;

import org.springframework.http.HttpStatus;

/**
 * Base de todas las excepciones de negocio del API.
 * Cada modulo define las suyas extendiendo de esta o de sus hijas.
 */
public abstract class ExcepcionRecyOps extends RuntimeException {

    private final HttpStatus estado;

    protected ExcepcionRecyOps(HttpStatus estado, String mensaje) {
        super(mensaje);
        this.estado = estado;
    }

    public HttpStatus getEstado() {
        return estado;
    }
}
