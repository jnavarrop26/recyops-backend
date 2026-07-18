package com.recyops.api.entrega.controller;

import com.recyops.api.comun.dtos.RespuestaPagina;
import com.recyops.api.entrega.dtos.CuerpoEntrega;
import com.recyops.api.entrega.dtos.RespuestaEntrega;
import com.recyops.api.entrega.dtos.RespuestaRecibo;
import com.recyops.api.entrega.enums.EstadoEntrega;
import com.recyops.api.entrega.interfaces.EntregaService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/entregas")
public class EntregaController {

    private final EntregaService entregaService;

    public EntregaController(EntregaService entregaService) {
        this.entregaService = entregaService;
    }

    @GetMapping
    public RespuestaPagina<RespuestaEntrega> listar(
            @RequestParam(required = false) UUID bodegaId,
            @RequestParam(required = false) UUID proveedorId,
            @RequestParam(required = false) EstadoEntrega estado,
            @RequestParam(required = false) LocalDate fechaDesde,
            @RequestParam(required = false) LocalDate fechaHasta,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return entregaService.listar(bodegaId, proveedorId, estado, fechaDesde, fechaHasta, page, size);
    }

    @GetMapping("/{id}")
    public RespuestaEntrega obtener(@PathVariable UUID id) {
        return entregaService.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RespuestaEntrega registrar(@Valid @RequestBody CuerpoEntrega cuerpo) {
        return entregaService.registrar(cuerpo);
    }

    @PatchMapping("/{id}/estado")
    public RespuestaEntrega cambiarEstado(@PathVariable UUID id, @RequestParam EstadoEntrega valor) {
        return entregaService.cambiarEstado(id, valor);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable UUID id) {
        entregaService.eliminar(id);
    }

    /** Recibo PDF de la entrega, para abrir o imprimir desde el cliente. */
    @GetMapping("/{id}/recibo")
    public ResponseEntity<byte[]> recibo(@PathVariable UUID id) {
        RespuestaRecibo recibo = entregaService.generarRecibo(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(recibo.nombreArchivo()).build().toString())
                .body(recibo.contenido());
    }
}
