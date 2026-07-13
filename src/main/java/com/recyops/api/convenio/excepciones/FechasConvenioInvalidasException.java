package com.recyops.api.convenio.excepciones;

import com.recyops.api.comun.excepciones.ReglaNegocioException;

public class FechasConvenioInvalidasException extends ReglaNegocioException {

    public FechasConvenioInvalidasException() {
        super("La fecha de fin del convenio no puede ser anterior a la fecha de inicio");
    }
}
