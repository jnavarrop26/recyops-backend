package com.recyops.api.usuario.controller;

import com.recyops.api.usuario.dtos.CuerpoEditarTrabajador;
import com.recyops.api.usuario.dtos.CuerpoTrabajador;
import com.recyops.api.usuario.dtos.RespuestaTrabajador;
import com.recyops.api.usuario.dtos.RespuestaTrabajadorCreado;
import com.recyops.api.usuario.enums.EstadoUsuario;
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

/** Gestion de trabajadores; protegido con rol ADMIN en SecurityConfig. */
@RestController
@RequestMapping("/api/admin/usuarios")
public class UsuarioAdminController {

    private final UsuarioService usuarioService;

    public UsuarioAdminController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public List<RespuestaTrabajador> listar() {
        return usuarioService.listarTrabajadores();
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
