package com.recyops.api.platform.excepciones;

import com.recyops.api.comun.excepciones.ExcepcionRecyOps;
import org.springframework.http.HttpStatus;

public class EmpresaYaExisteException extends ExcepcionRecyOps {

    public EmpresaYaExisteException(String campo, String valor) {
        super(HttpStatus.CONFLICT, "Ya existe una empresa con " + campo + " = " + valor);
    }
}
