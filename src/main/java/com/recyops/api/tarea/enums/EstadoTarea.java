package com.recyops.api.tarea.enums;

/** Columnas del tablero: el flujo normal avanza de izquierda a derecha. */
public enum EstadoTarea {
    PENDIENTE,
    EN_PROGRESO,
    COMPLETADA,
    REVISION,
    CANCELADA;

    /** Siguiente estado del flujo normal (CANCELADA queda fuera del flujo). */
    public EstadoTarea siguiente() {
        return switch (this) {
            case PENDIENTE -> EN_PROGRESO;
            case EN_PROGRESO -> COMPLETADA;
            case COMPLETADA -> REVISION;
            case REVISION, CANCELADA -> null;
        };
    }
}
