package com.recyops.api.usuario.controller;

import com.recyops.api.comun.dtos.RespuestaPagina;
import com.recyops.api.usuario.dtos.CuerpoEditarTrabajador;
import com.recyops.api.usuario.dtos.CuerpoTrabajador;
import com.recyops.api.usuario.dtos.RespuestaTrabajador;
import com.recyops.api.usuario.dtos.RespuestaTrabajadorCreado;
import com.recyops.api.usuario.enums.EstadoUsuario;
import com.recyops.api.usuario.interfaces.UsuarioService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
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

/** Gestion de trabajadores; protegido con rol ADMIN en SecurityConfig. */
@RestController
@RequestMapping("/api/admin/usuarios")
@Validated
public class UsuarioAdminController {

    private final UsuarioService usuarioService;

    public UsuarioAdminController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public RespuestaPagina<RespuestaTrabajador> listar(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return usuarioService.listarTrabajadores(page, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RespuestaTrabajadorCreado registrar(@Valid @RequestBody CuerpoTrabajador cuerpo) {
        return usuarioService.registrarTrabajador(cuerpo);
    }

    @PutMapping("/{id}")
    public RespuestaTrabajador editar(@PathVariable UUID id,
            @Valid @RequestBody CuerpoEditarTrabajador cuerpo) {
        return usuarioService.editarTrabajador(id, cuerpo);
    }

    @PatchMapping("/{id}/estado")
    public RespuestaTrabajador cambiarEstado(@PathVariable UUID id,
            @RequestParam EstadoUsuario valor) {
        return usuarioService.cambiarEstado(id, valor);
    }
}
