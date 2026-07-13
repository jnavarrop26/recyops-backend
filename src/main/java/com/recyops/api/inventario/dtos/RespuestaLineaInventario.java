package com.recyops.api.inventario.dtos;

import com.recyops.api.inventario.entity.LineaInventario;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Forma de linea de inventario que consume el cliente RecyOps. */
public record RespuestaLineaInventario(
        UUID id,
        UUID bodegaId,
        String bodegaNombre,
        UUID tipoMaterialId,
        String tipoMaterialNombre,
        String categoriaNombre,
        String unidadMedida,
        BigDecimal stockActual,
        BigDecimal stockMinimo,
        BigDecimal stockMaximo,
        boolean bajoMinimo,
        LocalDateTime fechaActualizacion) {

    public static RespuestaLineaInventario desde(LineaInventario linea) {
        return new RespuestaLineaInventario(
                linea.getId(),
                linea.getBodega().getId(),
                linea.getBodega().getNombre(),
                linea.getTipoMaterial().getId(),
                linea.getTipoMaterial().getNombre(),
                linea.getTipoMaterial().getCategoria().getNombre(),
                linea.getTipoMaterial().getUnidadMedida().name(),
                linea.getStockActual(),
                linea.getStockMinimo(),
                linea.getStockMaximo(),
                linea.estaBajoMinimo(),
                linea.getFechaActualizacion());
    }
}
