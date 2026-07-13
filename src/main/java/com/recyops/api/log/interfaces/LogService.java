package com.recyops.api.log.interfaces;

import com.recyops.api.log.dtos.RespuestaLineaLog;
import com.recyops.api.log.enums.TipoArchivoLog;
import java.util.List;

public interface LogService {

    /** Ultimas entradas del archivo indicado, de la mas reciente a la mas antigua. */
    List<RespuestaLineaLog> leer(TipoArchivoLog archivo, int lineas);
}
