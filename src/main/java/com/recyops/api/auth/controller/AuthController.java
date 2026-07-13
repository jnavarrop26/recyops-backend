package com.recyops.api.auth.controller;

import com.recyops.api.auth.dtos.CuerpoLogin;
import com.recyops.api.auth.dtos.CuerpoRecuperar;
import com.recyops.api.auth.dtos.CuerpoRefresh;
import com.recyops.api.auth.dtos.CuerpoRestablecer;
import com.recyops.api.auth.dtos.RespuestaLogin;
import com.recyops.api.auth.interfaces.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public RespuestaLogin login(@Valid @RequestBody CuerpoLogin cuerpo) {
        return authService.login(cuerpo);
    }

    @PostMapping("/refresh")
    public RespuestaLogin refrescar(@Valid @RequestBody CuerpoRefresh cuerpo) {
        return authService.refrescar(cuerpo);
    }

    @PostMapping("/recuperar")
    public void recuperar(@Valid @RequestBody CuerpoRecuperar cuerpo) {
        authService.recuperarPassword(cuerpo);
    }

    @PostMapping("/restablecer")
    public void restablecer(@Valid @RequestBody CuerpoRestablecer cuerpo) {
        authService.restablecerPassword(cuerpo);
    }
}
