package com.recyops.api.ingreso.dtos;

import com.recyops.api.ingreso.enums.MetodoPago;
import jakarta.validation.constraints.NotNull;

/** Datos para registrar el pago de un ingreso. */
public record CuerpoPago(
        @NotNull(message = "El metodo de pago es obligatorio") MetodoPago metodoPago) {
}
