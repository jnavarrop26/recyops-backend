package com.recyops.api.material.dtos;

import com.recyops.api.material.entity.Material;
import com.recyops.api.material.entity.OpcionCatalogo;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Forma de material que consume el cliente RecyOps (codigos + nombres). */
public record RespuestaMaterial(
        UUID id,
        String nombre,
        String categoriaCodigo,
        String categoriaNombre,
        String subcategoriaCodigo,
        String subcategoriaNombre,
        String resinaCodigo,
        String resinaNombre,
        String colorCodigo,
        String colorNombre,
        String unidadMedida,
        String unidadEmpaque,
        BigDecimal precioBase,
        BigDecimal factorCalidad,
        BigDecimal umbralMerma,
        boolean activo,
        LocalDateTime fechaCreacion) {

    public static RespuestaMaterial desde(Material material) {
        return new RespuestaMaterial(
                material.getId(),
                material.getNombre(),
                codigo(material.getCategoria()),
                nombre(material.getCategoria()),
                codigo(material.getSubcategoria()),
                nombre(material.getSubcategoria()),
                codigo(material.getResina()),
                nombre(material.getResina()),
                codigo(material.getColor()),
                nombre(material.getColor()),
                material.getUnidadMedida().name(),
                material.getUnidadEmpaque().name(),
                material.getPrecioBase(),
                material.getFactorCalidad(),
                material.getUmbralMerma(),
                material.isActivo(),
                material.getFechaCreacion());
    }

    private static String codigo(OpcionCatalogo opcion) {
        return opcion != null ? opcion.getCodigo() : null;
    }

    private static String nombre(OpcionCatalogo opcion) {
        return opcion != null ? opcion.getNombre() : null;
    }
}
