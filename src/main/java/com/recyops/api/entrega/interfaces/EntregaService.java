package com.recyops.api.entrega.interfaces;

import com.recyops.api.comun.dtos.RespuestaPagina;
import com.recyops.api.entrega.dtos.CuerpoEntrega;
import com.recyops.api.entrega.dtos.RespuestaEntrega;
import com.recyops.api.entrega.enums.EstadoEntrega;
import java.time.LocalDate;
import java.util.UUID;

public interface EntregaService {

    RespuestaPagina<RespuestaEntrega> listar(UUID bodegaId, UUID proveedorId, EstadoEntrega estado,
            LocalDate fechaDesde, LocalDate fechaHasta, int page, int size);

    RespuestaEntrega obtener(UUID id);

    RespuestaEntrega registrar(CuerpoEntrega cuerpo);

    RespuestaEntrega cambiarEstado(UUID id, EstadoEntrega valor);

    void eliminar(UUID id);
}
