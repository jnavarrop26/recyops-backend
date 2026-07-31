package com.recyops.api.ingreso.controller;

import com.recyops.api.comun.dtos.RespuestaPagina;
import com.recyops.api.ingreso.dtos.CuerpoIngreso;
import com.recyops.api.ingreso.dtos.CuerpoPago;
import com.recyops.api.ingreso.dtos.RespuestaIngreso;
import com.recyops.api.ingreso.enums.EstadoIngreso;
import com.recyops.api.ingreso.interfaces.IngresoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ingresos")
@Validated
public class IngresoController {

    private final IngresoService ingresoService;

    public IngresoController(IngresoService ingresoService) {
        this.ingresoService = ingresoService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public RespuestaPagina<RespuestaIngreso> historial(
            @RequestParam(required = false) LocalDate fechaDesde,
            @RequestParam(required = false) LocalDate fechaHasta,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ingresoService.historial(fechaDesde, fechaHasta, page, size);
    }

    /** Solo ADMIN: expone el ID secuencial, lo que permite enumeracion por fuerza bruta. */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public RespuestaIngreso obtener(@PathVariable Long id) {
        return ingresoService.obtener(id);
    }

    /** Identificador opaco para operarios: no revela la secuencia interna de IDs. */
    @GetMapping("/uuid/{uuid}")
    public RespuestaIngreso obtenerPorUuid(@PathVariable UUID uuid) {
        return ingresoService.obtenerPorUuid(uuid);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RespuestaIngreso registrar(@Valid @RequestBody CuerpoIngreso cuerpo) {
        return ingresoService.registrar(cuerpo);
    }

    @PatchMapping("/{id}/pago")
    @PreAuthorize("hasRole('ADMIN')")
    public RespuestaIngreso registrarPago(@PathVariable Long id, @Valid @RequestBody CuerpoPago cuerpo) {
        return ingresoService.registrarPago(id, cuerpo);
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasRole('ADMIN')")
    public RespuestaIngreso cambiarEstado(@PathVariable Long id, @RequestParam EstadoIngreso valor) {
        return ingresoService.cambiarEstado(id, valor);
    }

    @PatchMapping("/{id}/paso")
    @PreAuthorize("hasRole('ADMIN')")
    public RespuestaIngreso cambiarPaso(@PathVariable Long id, @RequestParam boolean valor) {
        return ingresoService.cambiarPaso(id, valor);
    }
}
