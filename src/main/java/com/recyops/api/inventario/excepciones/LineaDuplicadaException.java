package com.recyops.api.inventario.excepciones;

import com.recyops.api.comun.excepciones.ExcepcionRecyOps;
import org.springframework.http.HttpStatus;

public class LineaDuplicadaException extends ExcepcionRecyOps {

    public LineaDuplicadaException() {
        super(HttpStatus.CONFLICT, "Ya existe una linea de inventario para ese material en esa bodega");
    }
}
