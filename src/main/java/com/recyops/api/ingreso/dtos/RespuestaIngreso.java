package com.recyops.api.ingreso.dtos;

import com.recyops.api.ingreso.entity.IngresoMaterial;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Forma de ingreso que consume el historial y el recibo del cliente RecyOps. */
public record RespuestaIngreso(
        Long id,
        UUID uuid,
        LocalDateTime fecha,
        String cliente,
        String cedula,
        String bodegaDestino,
        String encargado,
        String placaVehiculo,
        BigDecimal pesoNetoTotal,
        BigDecimal total,
        String estado,
        List<RespuestaDetalleIngreso> detalles) {

    /** Version liviana para listados: sin los materiales del detalle. */
    public static RespuestaIngreso desde(IngresoMaterial ingreso) {
        return construir(ingreso, List.of());
    }

    /** Version completa para el detalle y el recibo impreso. */
    public static RespuestaIngreso conDetalles(IngresoMaterial ingreso) {
        return construir(ingreso, ingreso.getDetalles().stream()
                .map(RespuestaDetalleIngreso::desde)
                .toList());
    }

    private static RespuestaIngreso construir(IngresoMaterial ingreso,
            List<RespuestaDetalleIngreso> detalles) {
        return new RespuestaIngreso(
                ingreso.getId(),
                ingreso.getUuid(),
                ingreso.getFecha(),
                ingreso.getCliente(),
                ingreso.getCedula(),
                ingreso.getBodegaDestino(),
                ingreso.getEncargado(),
                ingreso.getPlacaVehiculo(),
                ingreso.getPesoNetoTotal(),
                ingreso.getTotal(),
                ingreso.getEstado().name(),
                detalles);
    }
}
