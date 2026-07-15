package com.recyops.api.platform.excepciones;

import com.recyops.api.comun.excepciones.ExcepcionRecyOps;
import org.springframework.http.HttpStatus;

public class SchemaInvalidoException extends ExcepcionRecyOps {

    public SchemaInvalidoException(String schema) {
        super(HttpStatus.BAD_REQUEST,
                "Nombre de esquema invalido '" + schema
                + "': solo minusculas, numeros y guion bajo, debe iniciar con letra");
    }
}
