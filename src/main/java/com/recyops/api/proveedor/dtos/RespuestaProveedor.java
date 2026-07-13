package com.recyops.api.proveedor.dtos;

import com.recyops.api.proveedor.entity.Proveedor;
import com.recyops.api.proveedor.enums.EstadoProveedor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record RespuestaProveedor(
        UUID id,
        String nombre,
        String nit,
        String contacto,
        String telefono,
        String email,
        String direccion,
        BigDecimal calificacion,
        EstadoProveedor estado,
        LocalDateTime fechaCreacion) {

    public static RespuestaProveedor desde(Proveedor proveedor) {
        return new RespuestaProveedor(
                proveedor.getId(),
                proveedor.getNombre(),
                proveedor.getNit(),
                proveedor.getContacto(),
                proveedor.getTelefono(),
                proveedor.getEmail(),
                proveedor.getDireccion(),
                proveedor.getCalificacion(),
                proveedor.getEstado(),
                proveedor.getFechaCreacion());
    }
}
