package com.recyops.api.proveedor.controller;

import com.recyops.api.comun.dtos.RespuestaPagina;
import com.recyops.api.proveedor.dtos.CuerpoProveedor;
import com.recyops.api.proveedor.dtos.RespuestaEntregaProveedor;
import com.recyops.api.proveedor.dtos.RespuestaProveedor;
import com.recyops.api.proveedor.enums.EstadoProveedor;
import com.recyops.api.proveedor.interfaces.ProveedorService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/api/proveedores")
public class ProveedorController {

    private final ProveedorService proveedorService;

    public ProveedorController(ProveedorService proveedorService) {
        this.proveedorService = proveedorService;
    }

    @GetMapping
    public RespuestaPagina<RespuestaProveedor> listar(
            @RequestParam(required = false) EstadoProveedor estado,
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) BigDecimal calificacionMin,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return proveedorService.listar(estado, nombre, calificacionMin, page, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RespuestaProveedor crear(@Valid @RequestBody CuerpoProveedor cuerpo) {
        return proveedorService.crear(cuerpo);
    }

    @PutMapping("/{id}")
    public RespuestaProveedor actualizar(@PathVariable UUID id, @Valid @RequestBody CuerpoProveedor cuerpo) {
        return proveedorService.actualizar(id, cuerpo);
    }

    @PatchMapping("/{id}/estado")
    public RespuestaProveedor cambiarEstado(@PathVariable UUID id, @RequestParam EstadoProveedor valor) {
        return proveedorService.cambiarEstado(id, valor);
    }

    @PatchMapping("/{id}/calificacion")
    public RespuestaProveedor calificar(@PathVariable UUID id, @RequestParam double valor) {
        return proveedorService.calificar(id, valor);
    }

    @GetMapping("/{id}/entregas")
    public List<RespuestaEntregaProveedor> entregas(@PathVariable UUID id) {
        return proveedorService.entregas(id);
    }
}
