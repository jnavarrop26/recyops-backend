package com.recyops.api.unit.convenio.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import tools.jackson.databind.ObjectMapper;
import com.recyops.api.comun.dtos.RespuestaPagina;
import com.recyops.api.config.FiltroRateLimit;
import com.recyops.api.convenio.controller.ConvenioController;
import com.recyops.api.convenio.dtos.CuerpoConvenio;
import com.recyops.api.convenio.dtos.RespuestaConvenio;
import com.recyops.api.convenio.enums.EstadoConvenio;
import com.recyops.api.convenio.enums.TipoConvenio;
import com.recyops.api.convenio.excepciones.ConvenioNoEncontradoException;
import com.recyops.api.convenio.excepciones.FechasConvenioInvalidasException;
import com.recyops.api.convenio.interfaces.ConvenioService;
import java.math.BigDecimal;
import java.time.LocalDate;
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
@WebMvcTest(controllers = ConvenioController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = FiltroRateLimit.class))
@AutoConfigureMockMvc(addFilters = false)
class ConvenioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ConvenioService convenioService;

    // ---------- listar ----------

    @Test
    void listar_sinParametros_devuelveOkConPagina() throws Exception {
        var respuesta = new RespuestaPagina<>(List.of(crearRespuestaConvenio()), 1, 1, 0, 20);
        when(convenioService.listar(null, null, null, 0, 20)).thenReturn(respuesta);

        mockMvc.perform(get("/api/convenios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void listar_conFiltros_lospasaAlServicio() throws Exception {
        when(convenioService.listar(EstadoConvenio.ACTIVO, TipoConvenio.COMPRA, "Reciclaje", 0, 20))
                .thenReturn(new RespuestaPagina<>(List.of(), 0, 0, 0, 20));

        mockMvc.perform(get("/api/convenios")
                        .param("estado", "ACTIVO")
                        .param("tipo", "COMPRA")
                        .param("nombre", "Reciclaje"))
                .andExpect(status().isOk());

        verify(convenioService).listar(EstadoConvenio.ACTIVO, TipoConvenio.COMPRA, "Reciclaje", 0, 20);
    }

    @Test
    void listar_pageNegativo_devuelveBadRequest() throws Exception {
        mockMvc.perform(get("/api/convenios").param("page", "-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listar_sizeMayorA100_devuelveBadRequest() throws Exception {
        mockMvc.perform(get("/api/convenios").param("size", "101"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listar_tipoInvalido_devuelveBadRequest() throws Exception {
        mockMvc.perform(get("/api/convenios").param("tipo", "NO_EXISTE"))
                .andExpect(status().isBadRequest());
    }

    // ---------- obtener ----------

    @Test
    void obtener_convenioExistente_devuelveOk() throws Exception {
        var id = UUID.randomUUID();
        when(convenioService.obtener(id)).thenReturn(crearRespuestaConvenio());

        mockMvc.perform(get("/api/convenios/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Convenio Reciclaje Norte"));
    }

    @Test
    void obtener_convenioInexistente_devuelveNotFound() throws Exception {
        var id = UUID.randomUUID();
        when(convenioService.obtener(id)).thenThrow(new ConvenioNoEncontradoException(id));

        mockMvc.perform(get("/api/convenios/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // ---------- crear ----------

    @Test
    void crear_cuerpoValido_devuelveCreated() throws Exception {
        var cuerpo = new CuerpoConvenio("Convenio Reciclaje Norte", TipoConvenio.COMPRA, UUID.randomUUID(),
                UUID.randomUUID(), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), BigDecimal.TEN,
                "Admin Uno", "Descripcion");
        when(convenioService.crear(any(CuerpoConvenio.class))).thenReturn(crearRespuestaConvenio());

        mockMvc.perform(post("/api/convenios")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Convenio Reciclaje Norte"));
    }

    @Test
    void crear_nombreEnBlanco_devuelveBadRequest() throws Exception {
        var cuerpo = new CuerpoConvenio("", TipoConvenio.COMPRA, null, null, LocalDate.of(2026, 1, 1), null, null,
                null, null);

        mockMvc.perform(post("/api/convenios")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isBadRequest());

        verify(convenioService, never()).crear(any());
    }

    @Test
    void crear_tipoNulo_devuelveBadRequest() throws Exception {
        var cuerpo = new CuerpoConvenio("Convenio Reciclaje Norte", null, null, null, LocalDate.of(2026, 1, 1),
                null, null, null, null);

        mockMvc.perform(post("/api/convenios")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isBadRequest());

        verify(convenioService, never()).crear(any());
    }

    @Test
    void crear_fechaInicioNula_devuelveBadRequest() throws Exception {
        var cuerpo = new CuerpoConvenio("Convenio Reciclaje Norte", TipoConvenio.COMPRA, null, null, null, null,
                null, null, null);

        mockMvc.perform(post("/api/convenios")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isBadRequest());

        verify(convenioService, never()).crear(any());
    }

    @Test
    void crear_fechasInvalidas_devuelveBadRequest() throws Exception {
        var cuerpo = new CuerpoConvenio("Convenio Reciclaje Norte", TipoConvenio.COMPRA, null, null,
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 1, 1), null, null, null);
        when(convenioService.crear(any(CuerpoConvenio.class))).thenThrow(new FechasConvenioInvalidasException());

        mockMvc.perform(post("/api/convenios")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isBadRequest());
    }

    // ---------- actualizar ----------

    @Test
    void actualizar_cuerpoValido_devuelveOk() throws Exception {
        var id = UUID.randomUUID();
        var cuerpo = new CuerpoConvenio("Convenio Reciclaje Norte", TipoConvenio.VENTA, null, null,
                LocalDate.of(2026, 1, 1), null, null, null, null);
        when(convenioService.actualizar(eq(id), any(CuerpoConvenio.class))).thenReturn(crearRespuestaConvenio());

        mockMvc.perform(put("/api/convenios/{id}", id)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isOk());
    }

    @Test
    void actualizar_convenioInexistente_devuelveNotFound() throws Exception {
        var id = UUID.randomUUID();
        var cuerpo = new CuerpoConvenio("Convenio Reciclaje Norte", TipoConvenio.VENTA, null, null,
                LocalDate.of(2026, 1, 1), null, null, null, null);
        when(convenioService.actualizar(eq(id), any(CuerpoConvenio.class)))
                .thenThrow(new ConvenioNoEncontradoException(id));

        mockMvc.perform(put("/api/convenios/{id}", id)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isNotFound());
    }

    // ---------- cambiarEstado ----------

    @Test
    void cambiarEstado_valorValido_devuelveOk() throws Exception {
        var id = UUID.randomUUID();
        when(convenioService.cambiarEstado(id, EstadoConvenio.SUSPENDIDO)).thenReturn(crearRespuestaConvenio());

        mockMvc.perform(patch("/api/convenios/{id}/estado", id).param("valor", "SUSPENDIDO"))
                .andExpect(status().isOk());
    }

    @Test
    void cambiarEstado_valorInvalido_devuelveBadRequest() throws Exception {
        var id = UUID.randomUUID();

        mockMvc.perform(patch("/api/convenios/{id}/estado", id).param("valor", "NO_EXISTE"))
                .andExpect(status().isBadRequest());

        verify(convenioService, never()).cambiarEstado(any(), any());
    }

    @Test
    void cambiarEstado_convenioInexistente_devuelveNotFound() throws Exception {
        var id = UUID.randomUUID();
        when(convenioService.cambiarEstado(id, EstadoConvenio.VENCIDO))
                .thenThrow(new ConvenioNoEncontradoException(id));

        mockMvc.perform(patch("/api/convenios/{id}/estado", id).param("valor", "VENCIDO"))
                .andExpect(status().isNotFound());
    }

    // ---------- helpers ----------

    private RespuestaConvenio crearRespuestaConvenio() {
        return new RespuestaConvenio(UUID.randomUUID(), "CONV-001", "Convenio Reciclaje Norte", TipoConvenio.COMPRA,
                UUID.randomUUID(), "Proveedor Uno", UUID.randomUUID(), "Bodega Central", LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31), BigDecimal.TEN, "Admin Uno", "Descripcion", EstadoConvenio.ACTIVO,
                LocalDateTime.now());
    }
}
