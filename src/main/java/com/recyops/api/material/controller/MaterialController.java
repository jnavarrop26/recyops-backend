package com.recyops.api.material.controller;

import com.recyops.api.comun.dtos.RespuestaPagina;
import com.recyops.api.material.dtos.CuerpoCambioActivo;
import com.recyops.api.material.dtos.CuerpoMaterial;
import com.recyops.api.material.dtos.RespuestaMaterial;
import com.recyops.api.material.dtos.RespuestaOpcionCatalogo;
import com.recyops.api.material.interfaces.MaterialService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
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
@RequestMapping("/api/materiales")
@Validated
public class MaterialController {

    private final MaterialService materialService;

    public MaterialController(MaterialService materialService) {
        this.materialService = materialService;
    }

    @GetMapping
    public RespuestaPagina<RespuestaMaterial> listar(
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) String resina,
            @RequestParam(required = false) String color,
            @RequestParam(required = false) String empaque,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return materialService.listar(categoria, resina, color, empaque, activo, page, size);
    }

    @GetMapping("/categorias")
    public List<RespuestaOpcionCatalogo> categorias() {
        return materialService.listarCategorias();
    }

    @GetMapping("/subcategorias")
    public List<RespuestaOpcionCatalogo> subcategorias(@RequestParam String categoria) {
        return materialService.listarSubcategorias(categoria);
    }

    @GetMapping("/resinas")
    public List<RespuestaOpcionCatalogo> resinas() {
        return materialService.listarResinas();
    }

    @GetMapping("/colores")
    public List<RespuestaOpcionCatalogo> colores() {
        return materialService.listarColores();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RespuestaMaterial crear(@Valid @RequestBody CuerpoMaterial cuerpo) {
        return materialService.crear(cuerpo);
    }

    @PutMapping("/{id}")
    public RespuestaMaterial actualizar(@PathVariable UUID id, @Valid @RequestBody CuerpoMaterial cuerpo) {
        return materialService.actualizar(id, cuerpo);
    }

    @PatchMapping("/{id}")
    public RespuestaMaterial cambiarActivo(@PathVariable UUID id,
            @Valid @RequestBody CuerpoCambioActivo cuerpo) {
        return materialService.cambiarActivo(id, cuerpo.activo());
    }
}
