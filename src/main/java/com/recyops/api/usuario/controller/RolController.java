package com.recyops.api.usuario.controller;

import com.recyops.api.usuario.dtos.RespuestaRol;
import com.recyops.api.usuario.interfaces.UsuarioService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/roles")
public class RolController {

    private final UsuarioService usuarioService;

    public RolController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public List<RespuestaRol> listar() {
        return usuarioService.listarRoles();
    }
}
