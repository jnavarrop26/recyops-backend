package com.recyops.api.log.controller;

import com.recyops.api.log.dtos.RespuestaLineaLog;
import com.recyops.api.log.enums.TipoArchivoLog;
import com.recyops.api.log.interfaces.LogService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Visor de logs para la UI; bajo /api/admin para exigir rol ADMIN. */
@RestController
@RequestMapping("/api/admin/logs")
public class LogController {

    private final LogService logService;

    public LogController(LogService logService) {
        this.logService = logService;
    }

    @GetMapping
    public List<RespuestaLineaLog> leer(
            @RequestParam(defaultValue = "GENERAL") TipoArchivoLog archivo,
            @RequestParam(defaultValue = "200") int lineas) {
        return logService.leer(archivo, Math.min(Math.max(lineas, 1), 1000));
    }
}
