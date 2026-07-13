package com.recyops.api.bodega.interfaces;

import com.recyops.api.bodega.dtos.CuerpoBodega;
import com.recyops.api.bodega.dtos.RespuestaBodega;
import com.recyops.api.bodega.enums.EstadoBodega;
import com.recyops.api.bodega.enums.TipoOrganizacion;
import com.recyops.api.comun.dtos.RespuestaPagina;
import java.util.UUID;

public interface BodegaService {

    RespuestaPagina<RespuestaBodega> listar(EstadoBodega estado, TipoOrganizacion tipoOrganizacion,
            String nombre, int page, int size);

    RespuestaBodega obtener(UUID id);

    RespuestaBodega crear(CuerpoBodega cuerpo);

    RespuestaBodega actualizar(UUID id, CuerpoBodega cuerpo);

    RespuestaBodega cambiarEstado(UUID id, EstadoBodega valor);
}
