package com.recyops.api.inventario.controller;

import com.recyops.api.comun.dtos.RespuestaPagina;
import com.recyops.api.inventario.dtos.CuerpoAjuste;
import com.recyops.api.inventario.dtos.CuerpoCrearLinea;
import com.recyops.api.inventario.dtos.CuerpoMerma;
import com.recyops.api.inventario.dtos.CuerpoTopes;
import com.recyops.api.inventario.dtos.RespuestaLineaInventario;
import com.recyops.api.inventario.dtos.RespuestaMovimiento;
import com.recyops.api.inventario.interfaces.InventarioService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventario")
public class InventarioController {

    private final InventarioService inventarioService;

    public InventarioController(InventarioService inventarioService) {
        this.inventarioService = inventarioService;
    }

    @GetMapping
    public RespuestaPagina<RespuestaLineaInventario> listar(
            @RequestParam UUID bodegaId,
            @RequestParam(required = false) UUID tipoMaterialId,
            @RequestParam(required = false) Boolean bajoMinimo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return inventarioService.listar(bodegaId, tipoMaterialId, bajoMinimo, page, size);
    }

    @GetMapping("/{id}")
    public RespuestaLineaInventario obtener(@PathVariable UUID id) {
        return inventarioService.obtener(id);
    }

    @GetMapping("/{id}/movimientos")
    public RespuestaPagina<RespuestaMovimiento> movimientos(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return inventarioService.movimientos(id, page, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RespuestaLineaInventario crearLinea(@Valid @RequestBody CuerpoCrearLinea cuerpo) {
        return inventarioService.crearLinea(cuerpo);
    }

    @PutMapping("/{id}")
    public RespuestaLineaInventario actualizarTopes(@PathVariable UUID id,
            @Valid @RequestBody CuerpoTopes cuerpo) {
        return inventarioService.actualizarTopes(id, cuerpo);
    }

    @PostMapping("/{id}/ajuste")
    public RespuestaLineaInventario registrarAjuste(@PathVariable UUID id,
            @Valid @RequestBody CuerpoAjuste cuerpo) {
        return inventarioService.registrarAjuste(id, cuerpo);
    }

    @PostMapping("/{id}/merma")
    public RespuestaLineaInventario registrarMerma(@PathVariable UUID id,
            @Valid @RequestBody CuerpoMerma cuerpo) {
        return inventarioService.registrarMerma(id, cuerpo);
    }
}
