package com.recyops.api.tarea.excepciones;

import com.recyops.api.comun.excepciones.ExcepcionRecyOps;
import org.springframework.http.HttpStatus;

/** Un operario solo puede mover las tareas que tiene asignadas. */
public class TareaAjenaException extends ExcepcionRecyOps {

    public TareaAjenaException() {
        super(HttpStatus.FORBIDDEN, "Esta tarea no esta asignada a ti");
    }
}
