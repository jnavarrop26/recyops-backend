package com.recyops.api.dashboard.controller;

import com.recyops.api.dashboard.dtos.RespuestaActividadDia;
import com.recyops.api.dashboard.dtos.RespuestaResumenDashboard;
import com.recyops.api.dashboard.interfaces.DashboardService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/resumen")
    public RespuestaResumenDashboard resumen() {
        return dashboardService.resumen();
    }

    @GetMapping("/actividad")
    public List<RespuestaActividadDia> actividad(@RequestParam(defaultValue = "7") int dias) {
        return dashboardService.actividadDiaria(Math.min(Math.max(dias, 1), 90));
    }
}
