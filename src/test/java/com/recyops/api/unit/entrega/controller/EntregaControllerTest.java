package com.recyops.api.unit.entrega.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import tools.jackson.databind.ObjectMapper;
import com.recyops.api.comun.dtos.RespuestaPagina;
import com.recyops.api.comun.excepciones.ReglaNegocioException;
import com.recyops.api.config.FiltroRateLimit;
import com.recyops.api.entrega.controller.EntregaController;
import com.recyops.api.entrega.dtos.CuerpoEntrega;
import com.recyops.api.entrega.dtos.CuerpoLineaEntrega;
import com.recyops.api.entrega.dtos.RespuestaEntrega;
import com.recyops.api.entrega.dtos.RespuestaRecibo;
import com.recyops.api.entrega.enums.EstadoEntrega;
import com.recyops.api.entrega.excepciones.EntregaNoEncontradaException;
import com.recyops.api.entrega.excepciones.TransicionEstadoInvalidaException;
import com.recyops.api.entrega.interfaces.EntregaService;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
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
@WebMvcTest(controllers = EntregaController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = FiltroRateLimit.class))
@AutoConfigureMockMvc(addFilters = false)
class EntregaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EntregaService entregaService;

    // ---------- listar ----------

    @Test
    void listar_sinParametros_usaPaginaPorDefecto() throws Exception {
        var respuesta = new RespuestaPagina<>(List.of(crearRespuestaEntrega()), 1, 1, 0, 20);
        when(entregaService.listar(null, null, null, null, null, 0, 20)).thenReturn(respuesta);

        mockMvc.perform(get("/api/entregas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void listar_conFiltros_lospasaAlServicio() throws Exception {
        var bodegaId = UUID.randomUUID();
        var convenioId = UUID.randomUUID();
        when(entregaService.listar(eq(bodegaId), eq(convenioId), eq(EstadoEntrega.RECIBIDA), any(), any(),
                eq(1), eq(10))).thenReturn(new RespuestaPagina<>(List.of(), 0, 0, 1, 10));

        mockMvc.perform(get("/api/entregas")
                        .param("bodegaId", bodegaId.toString())
                        .param("convenioId", convenioId.toString())
                        .param("estado", "RECIBIDA")
                        .param("fechaDesde", "2026-01-01")
                        .param("fechaHasta", "2026-01-31")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk());

        verify(entregaService).listar(bodegaId, convenioId, EstadoEntrega.RECIBIDA,
                java.time.LocalDate.of(2026, 1, 1), java.time.LocalDate.of(2026, 1, 31), 1, 10);
    }

    @Test
    void listar_pageNegativo_devuelveBadRequest() throws Exception {
        mockMvc.perform(get("/api/entregas").param("page", "-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listar_sizeMayorA100_devuelveBadRequest() throws Exception {
        mockMvc.perform(get("/api/entregas").param("size", "101"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listar_estadoInvalido_devuelveBadRequest() throws Exception {
        mockMvc.perform(get("/api/entregas").param("estado", "NO_EXISTE"))
                .andExpect(status().isBadRequest());
    }

    // ---------- obtener ----------

    @Test
    void obtener_entregaExistente_devuelveOk() throws Exception {
        var id = UUID.randomUUID();
        when(entregaService.obtener(id)).thenReturn(crearRespuestaEntrega());

        mockMvc.perform(get("/api/entregas/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("ENT-000001"));
    }

    @Test
    void obtener_entregaInexistente_devuelveNotFound() throws Exception {
        var id = UUID.randomUUID();
        when(entregaService.obtener(id)).thenThrow(new EntregaNoEncontradaException(id));

        mockMvc.perform(get("/api/entregas/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // ---------- registrar ----------

    @Test
    void registrar_cuerpoValido_devuelveCreated() throws Exception {
        var cuerpo = crearCuerpoEntrega();
        when(entregaService.registrar(any(CuerpoEntrega.class))).thenReturn(crearRespuestaEntrega());

        mockMvc.perform(post("/api/entregas")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").value("ENT-000001"));
    }

    @Test
    void registrar_convenioIdNulo_devuelveBadRequest() throws Exception {
        var cuerpo = new CuerpoEntrega(null, UUID.randomUUID(), UUID.randomUUID(), null,
                List.of(new CuerpoLineaEntrega(UUID.randomUUID(), BigDecimal.TEN)));

        mockMvc.perform(post("/api/entregas")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje", containsString("convenioId")));

        verify(entregaService, never()).registrar(any());
    }

    @Test
    void registrar_sinLineas_devuelveBadRequest() throws Exception {
        var cuerpo = new CuerpoEntrega(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null, List.of());

        mockMvc.perform(post("/api/entregas")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isBadRequest());

        verify(entregaService, never()).registrar(any());
    }

    @Test
    void registrar_pesoKgDeUnaLineaNoPositivo_devuelveBadRequest() throws Exception {
        var cuerpo = new CuerpoEntrega(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null,
                List.of(new CuerpoLineaEntrega(UUID.randomUUID(), BigDecimal.ZERO)));

        mockMvc.perform(post("/api/entregas")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isBadRequest());

        verify(entregaService, never()).registrar(any());
    }

    @Test
    void registrar_personaEntregaIdNula_devuelveBadRequest() throws Exception {
        var cuerpo = new CuerpoEntrega(UUID.randomUUID(), UUID.randomUUID(), null, null,
                List.of(new CuerpoLineaEntrega(UUID.randomUUID(), BigDecimal.TEN)));

        mockMvc.perform(post("/api/entregas")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isBadRequest());
    }

    // ---------- cambiarEstado ----------

    @Test
    void cambiarEstado_valorValido_devuelveOk() throws Exception {
        var id = UUID.randomUUID();
        when(entregaService.cambiarEstado(id, EstadoEntrega.EN_PROCESO)).thenReturn(crearRespuestaEntrega());

        mockMvc.perform(patch("/api/entregas/{id}/estado", id).param("valor", "EN_PROCESO"))
                .andExpect(status().isOk());
    }

    @Test
    void cambiarEstado_transicionInvalida_devuelveBadRequest() throws Exception {
        var id = UUID.randomUUID();
        when(entregaService.cambiarEstado(id, EstadoEntrega.DESPACHADA))
                .thenThrow(new TransicionEstadoInvalidaException(EstadoEntrega.RECIBIDA, EstadoEntrega.DESPACHADA));

        mockMvc.perform(patch("/api/entregas/{id}/estado", id).param("valor", "DESPACHADA"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cambiarEstado_entregaInexistente_devuelveNotFound() throws Exception {
        var id = UUID.randomUUID();
        when(entregaService.cambiarEstado(id, EstadoEntrega.EN_PROCESO))
                .thenThrow(new EntregaNoEncontradaException(id));

        mockMvc.perform(patch("/api/entregas/{id}/estado", id).param("valor", "EN_PROCESO"))
                .andExpect(status().isNotFound());
    }

    @Test
    void cambiarEstado_valorInvalido_devuelveBadRequest() throws Exception {
        var id = UUID.randomUUID();

        mockMvc.perform(patch("/api/entregas/{id}/estado", id).param("valor", "NO_EXISTE"))
                .andExpect(status().isBadRequest());

        verify(entregaService, never()).cambiarEstado(any(), any());
    }

    // ---------- eliminar ----------

    @Test
    void eliminar_entregaValida_devuelveNoContent() throws Exception {
        var id = UUID.randomUUID();

        mockMvc.perform(delete("/api/entregas/{id}", id))
                .andExpect(status().isNoContent());

        verify(entregaService).eliminar(id);
    }

    @Test
    void eliminar_entregaDespachada_devuelveBadRequest() throws Exception {
        var id = UUID.randomUUID();
        org.mockito.Mockito.doThrow(new ReglaNegocioException("La entrega ya fue despachada y no se puede eliminar"))
                .when(entregaService).eliminar(id);

        mockMvc.perform(delete("/api/entregas/{id}", id))
                .andExpect(status().isBadRequest());
    }

    @Test
    void eliminar_entregaInexistente_devuelveNotFound() throws Exception {
        var id = UUID.randomUUID();
        org.mockito.Mockito.doThrow(new EntregaNoEncontradaException(id)).when(entregaService).eliminar(id);

        mockMvc.perform(delete("/api/entregas/{id}", id))
                .andExpect(status().isNotFound());
    }

    // ---------- recibo ----------

    @Test
    void recibo_entregaExistente_devuelvePdf() throws Exception {
        var id = UUID.randomUUID();
        var contenido = "contenido-pdf-simulado".getBytes(StandardCharsets.UTF_8);
        when(entregaService.generarRecibo(id)).thenReturn(new RespuestaRecibo("ENT-000001", contenido));

        mockMvc.perform(get("/api/entregas/{id}/recibo", id))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition", containsString("recibo-ENT-000001.pdf")))
                .andExpect(result -> org.junit.jupiter.api.Assertions
                        .assertArrayEquals(contenido, result.getResponse().getContentAsByteArray()));
    }

    @Test
    void recibo_entregaInexistente_devuelveNotFound() throws Exception {
        var id = UUID.randomUUID();
        when(entregaService.generarRecibo(id)).thenThrow(new EntregaNoEncontradaException(id));

        mockMvc.perform(get("/api/entregas/{id}/recibo", id))
                .andExpect(status().isNotFound());
    }

    // ---------- helpers ----------

    private CuerpoEntrega crearCuerpoEntrega() {
        return new CuerpoEntrega(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                LocalDateTime.now(), List.of(new CuerpoLineaEntrega(UUID.randomUUID(), BigDecimal.TEN)));
    }

    private RespuestaEntrega crearRespuestaEntrega() {
        return new RespuestaEntrega(UUID.randomUUID(), "ENT-000001", UUID.randomUUID(), "Convenio Uno",
                UUID.randomUUID(), "Bodega Central", UUID.randomUUID(), "Juan Perez", "123456789",
                BigDecimal.TEN, EstadoEntrega.RECIBIDA, LocalDateTime.now(), "Admin Uno", List.of());
    }
}
