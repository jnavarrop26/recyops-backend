package com.recyops.api.log.dtos;

/**
 * Una entrada de log ya parseada para la UI. `detalle` trae las lineas
 * de continuacion (stack trace) cuando existen.
 */
public record RespuestaLineaLog(
        String fecha,
        String nivel,
        String usuario,
        String empresa,
        String logger,
        String mensaje,
        String detalle) {
}
