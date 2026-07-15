package com.recyops.api.platform.interfaces;

import com.recyops.api.platform.dtos.CuerpoNuevaEmpresa;
import com.recyops.api.platform.dtos.RespuestaEmpresaCreada;

public interface PlataformaService {

    RespuestaEmpresaCreada provisionarEmpresa(CuerpoNuevaEmpresa cuerpo);
}
