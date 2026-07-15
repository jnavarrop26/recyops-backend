package com.recyops.api.platform.controller;

import com.recyops.api.platform.dtos.CuerpoNuevaEmpresa;
import com.recyops.api.platform.dtos.RespuestaEmpresaCreada;
import com.recyops.api.platform.interfaces.PlataformaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/platform")
@PreAuthorize("hasRole('SUPERADMIN')")
public class PlataformaController {

    private final PlataformaService plataformaService;

    public PlataformaController(PlataformaService plataformaService) {
        this.plataformaService = plataformaService;
    }

    @PostMapping("/empresas")
    @ResponseStatus(HttpStatus.CREATED)
    public RespuestaEmpresaCreada provisionarEmpresa(@RequestBody @Valid CuerpoNuevaEmpresa cuerpo) {
        return plataformaService.provisionarEmpresa(cuerpo);
    }
}
