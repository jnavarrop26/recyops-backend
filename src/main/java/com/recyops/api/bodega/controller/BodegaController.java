package com.recyops.api.bodega.controller;

import com.recyops.api.bodega.dtos.CuerpoBodega;
import com.recyops.api.bodega.dtos.RespuestaBodega;
import com.recyops.api.bodega.enums.EstadoBodega;
import com.recyops.api.bodega.enums.TipoOrganizacion;
import com.recyops.api.bodega.interfaces.BodegaService;
import com.recyops.api.comun.dtos.RespuestaPagina;
import com.recyops.api.usuario.dtos.RespuestaUsuarioBodega;
import com.recyops.api.usuario.interfaces.UsuarioService;
import jakarta.validation.Valid;
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
@RequestMapping("/api/bodegas")
public class BodegaController {

    private final BodegaService bodegaService;
    private final UsuarioService usuarioService;

    public BodegaController(BodegaService bodegaService, UsuarioService usuarioService) {
        this.bodegaService = bodegaService;
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public RespuestaPagina<RespuestaBodega> listar(
            @RequestParam(required = false) EstadoBodega estado,
            @RequestParam(required = false) TipoOrganizacion tipoOrganizacion,
            @RequestParam(required = false) String nombre,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return bodegaService.listar(estado, tipoOrganizacion, nombre, page, size);
    }

    @GetMapping("/{id}")
    public RespuestaBodega obtener(@PathVariable UUID id) {
        return bodegaService.obtener(id);
    }

    @GetMapping("/{id}/usuarios")
    public List<RespuestaUsuarioBodega> usuarios(@PathVariable UUID id) {
        return usuarioService.listarPorBodega(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RespuestaBodega crear(@Valid @RequestBody CuerpoBodega cuerpo) {
        return bodegaService.crear(cuerpo);
    }

    @PutMapping("/{id}")
    public RespuestaBodega actualizar(@PathVariable UUID id, @Valid @RequestBody CuerpoBodega cuerpo) {
        return bodegaService.actualizar(id, cuerpo);
    }

    @PatchMapping("/{id}/estado")
    public RespuestaBodega cambiarEstado(@PathVariable UUID id, @RequestParam EstadoBodega valor) {
        return bodegaService.cambiarEstado(id, valor);
    }
}
