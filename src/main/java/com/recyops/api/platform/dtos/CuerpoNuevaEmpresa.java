package com.recyops.api.platform.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CuerpoNuevaEmpresa(
        @NotBlank @Size(max = 200) String nombre,
        @NotBlank @Size(max = 30) String nit,
        @NotBlank @Pattern(
                regexp = "^[a-z][a-z0-9_]{1,62}$",
                message = "Debe iniciar con letra, solo minusculas, numeros y guion bajo")
        String schemaNombre,
        @NotBlank @Email String adminEmail,
        @NotBlank @Size(max = 200) String adminNombreCompleto,
        @NotBlank @Size(max = 100) String adminUsername,
        String adminPassword
) {}
