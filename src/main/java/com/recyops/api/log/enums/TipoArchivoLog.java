package com.recyops.api.log.enums;

/** Archivos de log que expone el API para la vista de administracion. */
public enum TipoArchivoLog {
    GENERAL("recyops-api.log"),
    TRANSACCIONES("transacciones.log"),
    ERRORES("errores.log");

    private final String nombreArchivo;

    TipoArchivoLog(String nombreArchivo) {
        this.nombreArchivo = nombreArchivo;
    }

    public String getNombreArchivo() {
        return nombreArchivo;
    }
}
