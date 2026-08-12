package com.recyops.api.unit.log.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.recyops.api.config.FiltroRateLimit;
import com.recyops.api.log.controller.LogController;
import com.recyops.api.log.dtos.RespuestaLineaLog;
import com.recyops.api.log.enums.TipoArchivoLog;
import com.recyops.api.log.interfaces.LogService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = LogController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = FiltroRateLimit.class))
@AutoConfigureMockMvc(addFilters = false)
class LogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LogService logService;

    @Test
    void leer_sinParametros_usaGeneralY200LineasPorDefecto() throws Exception {
        var linea = new RespuestaLineaLog("2026-03-01 10:00:00", "INFO", "admin", "empresa_demo",
                "TareaService", "Tarea creada", null);
        when(logService.leer(TipoArchivoLog.GENERAL, 200)).thenReturn(List.of(linea));

        mockMvc.perform(get("/api/admin/logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].mensaje").value("Tarea creada"));
    }

    @Test
    void leer_archivoYLineasDados_lospasaAlServicio() throws Exception {
        when(logService.leer(TipoArchivoLog.ERRORES, 50)).thenReturn(List.of());

        mockMvc.perform(get("/api/admin/logs").param("archivo", "ERRORES").param("lineas", "50"))
                .andExpect(status().isOk());
    }

    @Test
    void leer_lineasPorEncimaDelLimite_seAcotanA1000() throws Exception {
        when(logService.leer(eq(TipoArchivoLog.GENERAL), eq(1000))).thenReturn(List.of());

        mockMvc.perform(get("/api/admin/logs").param("lineas", "5000"))
                .andExpect(status().isOk());
    }

    @Test
    void leer_lineasNegativas_seAcotanA1() throws Exception {
        when(logService.leer(eq(TipoArchivoLog.GENERAL), eq(1))).thenReturn(List.of());

        mockMvc.perform(get("/api/admin/logs").param("lineas", "-5"))
                .andExpect(status().isOk());
    }

    @Test
    void leer_archivoInvalido_devuelveBadRequest() throws Exception {
        mockMvc.perform(get("/api/admin/logs").param("archivo", "NO_EXISTE"))
                .andExpect(status().isBadRequest());
    }
}
