package com.recyops.api.ingreso.controller;

import com.recyops.api.ingreso.dtos.CuerpoIngreso;
import com.recyops.api.ingreso.dtos.RespuestaIngreso;
import com.recyops.api.ingreso.interfaces.IngresoService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ingresos")
public class IngresoController {

    private final IngresoService ingresoService;

    public IngresoController(IngresoService ingresoService) {
        this.ingresoService = ingresoService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<RespuestaIngreso> historial(
            @RequestParam(required = false) LocalDate fechaDesde,
            @RequestParam(required = false) LocalDate fechaHasta) {
        return ingresoService.historial(fechaDesde, fechaHasta);
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
}
