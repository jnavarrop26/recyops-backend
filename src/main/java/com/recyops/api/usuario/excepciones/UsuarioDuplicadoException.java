package com.recyops.api.usuario.excepciones;

import com.recyops.api.comun.excepciones.ExcepcionRecyOps;
import org.springframework.http.HttpStatus;

public class UsuarioDuplicadoException extends ExcepcionRecyOps {

    public UsuarioDuplicadoException(String campo, String valor) {
        super(HttpStatus.CONFLICT, "Ya existe un usuario con " + campo + " '" + valor + "'");
    }
}
