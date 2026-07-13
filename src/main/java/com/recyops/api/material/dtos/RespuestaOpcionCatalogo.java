package com.recyops.api.material.dtos;

import com.recyops.api.material.entity.OpcionCatalogo;

/** Opcion para los selects del cliente (categorias, subcategorias, resinas, colores). */
public record RespuestaOpcionCatalogo(String codigo, String nombre, Boolean esTermoplastico) {

    public static RespuestaOpcionCatalogo desde(OpcionCatalogo opcion) {
        return new RespuestaOpcionCatalogo(opcion.getCodigo(), opcion.getNombre(), opcion.getEsTermoplastico());
    }
}
