package com.recyops.api.convenio.interfaces;

import com.recyops.api.comun.dtos.RespuestaPagina;
import com.recyops.api.convenio.dtos.CuerpoConvenio;
import com.recyops.api.convenio.dtos.RespuestaConvenio;
import com.recyops.api.convenio.enums.EstadoConvenio;
import com.recyops.api.convenio.enums.TipoConvenio;
import java.util.UUID;

public interface ConvenioService {

    RespuestaPagina<RespuestaConvenio> listar(EstadoConvenio estado, TipoConvenio tipo, String nombre,
            int page, int size);

    RespuestaConvenio obtener(UUID id);

    RespuestaConvenio crear(CuerpoConvenio cuerpo);

    RespuestaConvenio actualizar(UUID id, CuerpoConvenio cuerpo);

    RespuestaConvenio cambiarEstado(UUID id, EstadoConvenio valor);
}
