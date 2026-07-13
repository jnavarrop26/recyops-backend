package com.recyops.api.proveedor.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** POST y PUT /api/proveedores. */
public record CuerpoProveedor(
        @NotBlank String nombre,
        @NotBlank String nit,
        String contacto,
        String telefono,
        @Email String email,
        String direccion) {
}
