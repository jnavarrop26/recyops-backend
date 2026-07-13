package com.recyops.api.bodega.dtos;

import com.recyops.api.bodega.entity.Bodega;
import com.recyops.api.bodega.enums.EstadoBodega;
import java.time.LocalDateTime;
import java.util.UUID;

/** Forma de bodega que consume el cliente RecyOps. */
public record RespuestaBodega(
        UUID id,
        String nombre,
        String direccion,
        String telefono,
        String email,
        String nit,
        Double latitud,
        Double longitud,
        String tipoOrganizacion,
        EstadoBodega estado,
        LocalDateTime fechaCreacion) {

    public static RespuestaBodega desde(Bodega bodega) {
        return new RespuestaBodega(
                bodega.getId(),
                bodega.getNombre(),
                bodega.getDireccion(),
                bodega.getTelefono(),
                bodega.getEmail(),
                bodega.getNit(),
                bodega.getLatitud(),
                bodega.getLongitud(),
                bodega.getTipoOrganizacion().name(),
                bodega.getEstado(),
                bodega.getFechaCreacion());
    }
}
