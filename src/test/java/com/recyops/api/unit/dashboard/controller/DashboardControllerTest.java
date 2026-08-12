package com.recyops.api.unit.dashboard.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.recyops.api.config.FiltroRateLimit;
import com.recyops.api.dashboard.controller.DashboardController;
import com.recyops.api.dashboard.dtos.RespuestaActividadDia;
import com.recyops.api.dashboard.dtos.RespuestaResumenDashboard;
import com.recyops.api.dashboard.interfaces.DashboardService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Slice de MVC puro (sin @PreAuthorize en este controlador; la autorizacion
 * real vive en las reglas por URL de SecurityConfig, fuera de este slice).
 * Los filtros de seguridad se desactivan para aislar mapeo/serializacion.
 */
@WebMvcTest(controllers = DashboardController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = FiltroRateLimit.class))
@AutoConfigureMockMvc(addFilters = false)
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardService dashboardService;

    @Test
    void resumen_devuelveOkConIndicadores() throws Exception {
        var resumen = new RespuestaResumenDashboard(5, 3, 10, 2, 1);
        when(dashboardService.resumen()).thenReturn(resumen);

        mockMvc.perform(get("/api/dashboard/resumen"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalBodegas").value(5))
                .andExpect(jsonPath("$.entregasHoy").value(2));
    }

    @Test
    void actividad_sinParametro_usaSieteDiasPorDefecto() throws Exception {
        var dia = new RespuestaActividadDia(LocalDate.of(2026, 3, 1), 4, BigDecimal.TEN, BigDecimal.valueOf(1000),
                2, BigDecimal.ONE);
        when(dashboardService.actividadDiaria(7)).thenReturn(List.of(dia));

        mockMvc.perform(get("/api/dashboard/actividad"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ingresos").value(4));
    }

    @Test
    void actividad_diasDado_lopasaAlServicio() throws Exception {
        when(dashboardService.actividadDiaria(30)).thenReturn(List.of());

        mockMvc.perform(get("/api/dashboard/actividad").param("dias", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void actividad_diasFueraDeRango_seEnvianTalCualAlServicio() throws Exception {
        // El controlador acota (Math.min/Math.max) antes de llamar al service;
        // 9999 pedido -> el service recibe 90 (limite superior)
        when(dashboardService.actividadDiaria(eq(90))).thenReturn(List.of());

        mockMvc.perform(get("/api/dashboard/actividad").param("dias", "9999"))
                .andExpect(status().isOk());
    }
}
