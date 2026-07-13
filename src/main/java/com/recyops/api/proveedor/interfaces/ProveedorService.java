package com.recyops.api.proveedor.interfaces;

import com.recyops.api.comun.dtos.RespuestaPagina;
import com.recyops.api.proveedor.dtos.CuerpoProveedor;
import com.recyops.api.proveedor.dtos.RespuestaEntregaProveedor;
import com.recyops.api.proveedor.dtos.RespuestaProveedor;
import com.recyops.api.proveedor.enums.EstadoProveedor;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface ProveedorService {

    RespuestaPagina<RespuestaProveedor> listar(EstadoProveedor estado, String nombre,
            BigDecimal calificacionMin, int page, int size);

    RespuestaProveedor crear(CuerpoProveedor cuerpo);

    RespuestaProveedor actualizar(UUID id, CuerpoProveedor cuerpo);

    RespuestaProveedor cambiarEstado(UUID id, EstadoProveedor valor);

    RespuestaProveedor calificar(UUID id, double valor);

    List<RespuestaEntregaProveedor> entregas(UUID id);
}
