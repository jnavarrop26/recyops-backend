package com.recyops.api.comun.dtos;

import java.time.LocalDateTime;

/**
 * Cuerpo uniforme de error que devuelve el API.
 */
public record ErrorApi(
        LocalDateTime timestamp,
        int status,
        String error,
        String mensaje,
        String ruta) {

    public static ErrorApi de(int status, String error, String mensaje, String ruta) {
        return new ErrorApi(LocalDateTime.now(), status, error, mensaje, ruta);
    }
}
