package com.recyops.api.entrega.dtos;

import com.recyops.api.entrega.entity.LineaEntrega;
import java.math.BigDecimal;
import java.util.UUID;

public record RespuestaLineaEntrega(
        UUID tipoMaterialId,
        String tipoMaterialNombre,
        BigDecimal pesoKg) {

    public static RespuestaLineaEntrega desde(LineaEntrega linea) {
        return new RespuestaLineaEntrega(
                linea.getTipoMaterial().getId(),
                linea.getTipoMaterial().getNombre(),
                linea.getPesoKg());
    }
}
