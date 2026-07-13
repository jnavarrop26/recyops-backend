package com.recyops.api.tarea.excepciones;

import com.recyops.api.comun.excepciones.ReglaNegocioException;
import com.recyops.api.tarea.enums.EstadoTarea;

public class TransicionTareaInvalidaException extends ReglaNegocioException {

    public TransicionTareaInvalidaException(EstadoTarea actual, EstadoTarea solicitado) {
        super("No puedes pasar la tarea de " + actual + " a " + solicitado
                + "; el flujo es PENDIENTE > EN_PROGRESO > COMPLETADA");
    }
}
