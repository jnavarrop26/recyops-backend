package com.recyops.api.material.interfaces;

import com.recyops.api.comun.dtos.RespuestaPagina;
import com.recyops.api.material.dtos.CuerpoMaterial;
import com.recyops.api.material.dtos.RespuestaMaterial;
import com.recyops.api.material.dtos.RespuestaOpcionCatalogo;
import java.util.List;
import java.util.UUID;

public interface MaterialService {

    RespuestaPagina<RespuestaMaterial> listar(String categoria, String resina, String color,
            String empaque, Boolean activo, int page, int size);

    RespuestaMaterial crear(CuerpoMaterial cuerpo);

    RespuestaMaterial actualizar(UUID id, CuerpoMaterial cuerpo);

    RespuestaMaterial cambiarActivo(UUID id, boolean activo);

    List<RespuestaOpcionCatalogo> listarCategorias();

    List<RespuestaOpcionCatalogo> listarSubcategorias(String categoria);

    List<RespuestaOpcionCatalogo> listarResinas();

    List<RespuestaOpcionCatalogo> listarColores();
}
