package com.recyops.api.platform.dtos;

import java.util.UUID;

public record RespuestaEmpresaCreada(
        UUID empresaId,
        String nombre,
        String nit,
        String schemaNombre,
        UUID adminSupabaseId,
        String adminEmail,
        String passwordTemporal
) {}
