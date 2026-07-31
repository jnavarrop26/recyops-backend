package com.recyops.api.ingreso.interfaces;

import com.recyops.api.comun.dtos.RespuestaPagina;
import com.recyops.api.ingreso.dtos.CuerpoIngreso;
import com.recyops.api.ingreso.dtos.CuerpoPago;
import com.recyops.api.ingreso.dtos.RespuestaIngreso;
import com.recyops.api.ingreso.enums.EstadoIngreso;
import java.time.LocalDate;
import java.util.UUID;

public interface IngresoService {

    RespuestaPagina<RespuestaIngreso> historial(LocalDate fechaDesde, LocalDate fechaHasta, int page, int size);

    RespuestaIngreso obtener(Long id);

    /** Busqueda por el identificador publico (no enumerable) del recibo. */
    RespuestaIngreso obtenerPorUuid(UUID uuid);

    RespuestaIngreso registrar(CuerpoIngreso cuerpo);

    /** Marca el ingreso como pagado, o corrige el metodo si ya estaba pagado. */
    RespuestaIngreso registrarPago(Long id, CuerpoPago cuerpo);

    /** Cambia el estado operativo del lote (tablero del historial). */
    RespuestaIngreso cambiarEstado(Long id, EstadoIngreso valor);

    /** Marca si el lote paso o no. */
    RespuestaIngreso cambiarPaso(Long id, boolean valor);
}
