package com.recyops.api.ingreso.interfaces;

import com.recyops.api.ingreso.dtos.CuerpoIngreso;
import com.recyops.api.ingreso.dtos.RespuestaIngreso;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface IngresoService {

    List<RespuestaIngreso> historial(LocalDate fechaDesde, LocalDate fechaHasta);

    RespuestaIngreso obtener(Long id);

    /** Busqueda por el identificador publico (no enumerable) del recibo. */
    RespuestaIngreso obtenerPorUuid(UUID uuid);

    RespuestaIngreso registrar(CuerpoIngreso cuerpo);
}
