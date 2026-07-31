package com.recyops.api.convenio.controller;

import com.recyops.api.comun.dtos.RespuestaPagina;
import com.recyops.api.convenio.dtos.CuerpoConvenio;
import com.recyops.api.convenio.dtos.RespuestaConvenio;
import com.recyops.api.convenio.enums.EstadoConvenio;
import com.recyops.api.convenio.enums.TipoConvenio;
import com.recyops.api.convenio.interfaces.ConvenioService;
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

@RestController
@RequestMapping("/api/convenios")
@Validated
public class ConvenioController {

    private final ConvenioService convenioService;

    public ConvenioController(ConvenioService convenioService) {
        this.convenioService = convenioService;
    }

    @GetMapping
    public RespuestaPagina<RespuestaConvenio> listar(
            @RequestParam(required = false) EstadoConvenio estado,
            @RequestParam(required = false) TipoConvenio tipo,
            @RequestParam(required = false) String nombre,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return convenioService.listar(estado, tipo, nombre, page, size);
    }

    @GetMapping("/{id}")
    public RespuestaConvenio obtener(@PathVariable UUID id) {
        return convenioService.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RespuestaConvenio crear(@Valid @RequestBody CuerpoConvenio cuerpo) {
        return convenioService.crear(cuerpo);
    }

    @PutMapping("/{id}")
    public RespuestaConvenio actualizar(@PathVariable UUID id, @Valid @RequestBody CuerpoConvenio cuerpo) {
        return convenioService.actualizar(id, cuerpo);
    }

    @PatchMapping("/{id}/estado")
    public RespuestaConvenio cambiarEstado(@PathVariable UUID id, @RequestParam EstadoConvenio valor) {
        return convenioService.cambiarEstado(id, valor);
    }
}
