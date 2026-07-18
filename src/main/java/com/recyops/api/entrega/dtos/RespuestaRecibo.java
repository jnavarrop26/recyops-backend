package com.recyops.api.entrega.dtos;

/** Recibo PDF generado para una entrega. */
public record RespuestaRecibo(String codigo, byte[] contenido) {

    /** Nombre de archivo sugerido al cliente, ej. recibo-ENT-000042.pdf. */
    public String nombreArchivo() {
        return "recibo-" + codigo + ".pdf";
    }
}
