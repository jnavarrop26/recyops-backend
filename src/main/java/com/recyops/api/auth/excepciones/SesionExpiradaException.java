package com.recyops.api.auth.excepciones;

import com.recyops.api.comun.excepciones.ExcepcionRecyOps;
import org.springframework.http.HttpStatus;

/** El refresh token ya no es valido: hay que iniciar sesion de nuevo. */
public class SesionExpiradaException extends ExcepcionRecyOps {

    public SesionExpiradaException() {
        super(HttpStatus.UNAUTHORIZED, "La sesion expiro; inicia sesion de nuevo");
    }
}
