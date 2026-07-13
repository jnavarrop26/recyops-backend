package com.recyops.api.auth.excepciones;

import com.recyops.api.comun.excepciones.ExcepcionRecyOps;
import org.springframework.http.HttpStatus;

public class CredencialesInvalidasException extends ExcepcionRecyOps {

    public CredencialesInvalidasException() {
        super(HttpStatus.UNAUTHORIZED, "Usuario o contrasena incorrectos");
    }
}
