package com.recyops.api.entrega.enums;

/** Flujo de trazabilidad de una entrega de material. */
public enum EstadoEntrega {
    RECIBIDA,
    EN_PROCESO,
    PROCESADA,
    DESPACHADA;

    /** Siguiente estado valido del flujo, o null si es el ultimo. */
    public EstadoEntrega siguiente() {
        EstadoEntrega[] valores = values();
        int indice = ordinal();
        return indice < valores.length - 1 ? valores[indice + 1] : null;
    }
}
