package com.recyops.api.tarea.controller;

import com.recyops.api.comun.dtos.RespuestaPagina;
import com.recyops.api.tarea.dtos.CuerpoAvance;
import com.recyops.api.tarea.dtos.CuerpoTarea;
import com.recyops.api.tarea.dtos.RespuestaAvance;
import com.recyops.api.tarea.dtos.RespuestaTarea;
import com.recyops.api.tarea.enums.EstadoTarea;
import com.recyops.api.tarea.interfaces.TareaService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tareas")
@Validated
public class TareaController {

    private final TareaService tareaService;

    public TareaController(TareaService tareaService) {
        this.tareaService = tareaService;
    }

    /** Tablero completo: solo administradores. */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public RespuestaPagina<RespuestaTarea> listar(
            @RequestParam(required = false) EstadoTarea estado,
            @RequestParam(required = false) UUID asignadoId,
            @RequestParam(required = false) UUID bodegaId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return tareaService.listar(estado, asignadoId, bodegaId, page, size);
    }

    /** Las tareas del usuario autenticado (admin u operario). */
    @GetMapping("/mias")
    public List<RespuestaTarea> misTareas() {
        return tareaService.misTareas();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public RespuestaTarea crear(@Valid @RequestBody CuerpoTarea cuerpo) {
        return tareaService.crear(cuerpo);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public RespuestaTarea editar(@PathVariable UUID id, @Valid @RequestBody CuerpoTarea cuerpo) {
        return tareaService.editar(id, cuerpo);
    }

    /** El service valida: admin mueve libre; operario solo avanza las suyas. */
    @PatchMapping("/{id}/estado")
    public RespuestaTarea cambiarEstado(@PathVariable UUID id, @RequestParam EstadoTarea valor) {
        return tareaService.cambiarEstado(id, valor);
    }

    /** Bitacora de avances (admin o asignado; el service valida). */
    @GetMapping("/{id}/avances")
    public List<RespuestaAvance> avances(@PathVariable UUID id) {
        return tareaService.listarAvances(id);
    }

    @PostMapping("/{id}/avances")
    @ResponseStatus(HttpStatus.CREATED)
    public RespuestaAvance agregarAvance(@PathVariable UUID id,
            @Valid @RequestBody CuerpoAvance cuerpo) {
        return tareaService.agregarAvance(id, cuerpo);
    }

    /** Borrado definitivo: solo admin y solo tareas en REVISION. */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable UUID id) {
        tareaService.eliminar(id);
    }
}
