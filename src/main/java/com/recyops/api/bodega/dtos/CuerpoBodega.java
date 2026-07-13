package com.recyops.api.bodega.dtos;

import com.recyops.api.bodega.enums.TipoOrganizacion;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Cuerpo de creacion/actualizacion de bodega (POST y PUT /api/bodegas). */
public record CuerpoBodega(
        @NotBlank String nombre,
        @NotBlank String direccion,
        String telefono,
        @Email String email,
        @NotBlank String nit,
        Double latitud,
        Double longitud,
        @NotNull TipoOrganizacion tipoOrganizacion) {
}
