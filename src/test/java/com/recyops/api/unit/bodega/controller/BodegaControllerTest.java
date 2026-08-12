package com.recyops.api.unit.bodega.controller;

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
import com.recyops.api.bodega.controller.BodegaController;
import com.recyops.api.bodega.dtos.CuerpoBodega;
import com.recyops.api.bodega.dtos.RespuestaBodega;
import com.recyops.api.bodega.enums.EstadoBodega;
import com.recyops.api.bodega.enums.TipoOrganizacion;
import com.recyops.api.bodega.excepciones.BodegaNoEncontradaException;
import com.recyops.api.bodega.interfaces.BodegaService;
import com.recyops.api.comun.dtos.RespuestaPagina;
import com.recyops.api.config.FiltroRateLimit;
import com.recyops.api.usuario.dtos.RespuestaUsuarioBodega;
import com.recyops.api.usuario.interfaces.UsuarioService;
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
@WebMvcTest(controllers = BodegaController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = FiltroRateLimit.class))
@AutoConfigureMockMvc(addFilters = false)
class BodegaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BodegaService bodegaService;

    @MockitoBean
    private UsuarioService usuarioService;

    // ---------- listar ----------

    @Test
    void listar_sinParametros_devuelveOkConPagina() throws Exception {
        var respuesta = new RespuestaPagina<>(List.of(crearRespuestaBodega()), 1, 1, 0, 20);
        when(bodegaService.listar(null, null, null, 0, 20)).thenReturn(respuesta);

        mockMvc.perform(get("/api/bodegas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void listar_conFiltros_lospasaAlServicio() throws Exception {
        when(bodegaService.listar(EstadoBodega.ACTIVA, TipoOrganizacion.PROPIA, "Central", 0, 20))
                .thenReturn(new RespuestaPagina<>(List.of(), 0, 0, 0, 20));

        mockMvc.perform(get("/api/bodegas")
                        .param("estado", "ACTIVA")
                        .param("tipoOrganizacion", "PROPIA")
                        .param("nombre", "Central"))
                .andExpect(status().isOk());

        verify(bodegaService).listar(EstadoBodega.ACTIVA, TipoOrganizacion.PROPIA, "Central", 0, 20);
    }

    @Test
    void listar_pageNegativo_devuelveBadRequest() throws Exception {
        mockMvc.perform(get("/api/bodegas").param("page", "-1"))
                .andExpect(status().isBadRequest());

        verify(bodegaService, never()).listar(any(), any(), any(), org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void listar_sizeMayorA100_devuelveBadRequest() throws Exception {
        mockMvc.perform(get("/api/bodegas").param("size", "101"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listar_estadoInvalido_devuelveBadRequest() throws Exception {
        mockMvc.perform(get("/api/bodegas").param("estado", "NO_EXISTE"))
                .andExpect(status().isBadRequest());
    }

    // ---------- obtener ----------

    @Test
    void obtener_bodegaExistente_devuelveOk() throws Exception {
        var id = UUID.randomUUID();
        when(bodegaService.obtener(id)).thenReturn(crearRespuestaBodega());

        mockMvc.perform(get("/api/bodegas/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Bodega Central"));
    }

    @Test
    void obtener_bodegaInexistente_devuelveNotFound() throws Exception {
        var id = UUID.randomUUID();
        when(bodegaService.obtener(id)).thenThrow(new BodegaNoEncontradaException(id));

        mockMvc.perform(get("/api/bodegas/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // ---------- usuarios ----------

    @Test
    void usuarios_bodegaExistente_devuelveOk() throws Exception {
        var id = UUID.randomUUID();
        var usuario = new RespuestaUsuarioBodega(UUID.randomUUID(), "Maria Lopez", "mlopez", "OPERARIO", "ACTIVO");
        when(usuarioService.listarPorBodega(id)).thenReturn(List.of(usuario));

        mockMvc.perform(get("/api/bodegas/{id}/usuarios", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombreCompleto").value("Maria Lopez"));
    }

    // ---------- crear ----------

    @Test
    void crear_cuerpoValido_devuelveCreated() throws Exception {
        var cuerpo = new CuerpoBodega("Bodega Central", "Calle 1 # 2-3", "3001234567", "central@recyops.com",
                "900123456-1", 4.6, -74.0, TipoOrganizacion.PROPIA);
        when(bodegaService.crear(any(CuerpoBodega.class))).thenReturn(crearRespuestaBodega());

        mockMvc.perform(post("/api/bodegas")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Bodega Central"));
    }

    @Test
    void crear_nombreEnBlanco_devuelveBadRequest() throws Exception {
        var cuerpo = new CuerpoBodega("", "Calle 1 # 2-3", null, null, "900123456-1", null, null,
                TipoOrganizacion.PROPIA);

        mockMvc.perform(post("/api/bodegas")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isBadRequest());

        verify(bodegaService, never()).crear(any());
    }

    @Test
    void crear_tipoOrganizacionNulo_devuelveBadRequest() throws Exception {
        var cuerpo = new CuerpoBodega("Bodega Central", "Calle 1 # 2-3", null, null, "900123456-1", null, null,
                null);

        mockMvc.perform(post("/api/bodegas")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isBadRequest());

        verify(bodegaService, never()).crear(any());
    }

    @Test
    void crear_emailInvalido_devuelveBadRequest() throws Exception {
        var cuerpo = new CuerpoBodega("Bodega Central", "Calle 1 # 2-3", null, "no-es-un-correo", "900123456-1",
                null, null, TipoOrganizacion.PROPIA);

        mockMvc.perform(post("/api/bodegas")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isBadRequest());

        verify(bodegaService, never()).crear(any());
    }

    // ---------- actualizar ----------

    @Test
    void actualizar_cuerpoValido_devuelveOk() throws Exception {
        var id = UUID.randomUUID();
        var cuerpo = new CuerpoBodega("Bodega Central", "Calle 1 # 2-3", null, null, "900123456-1", null, null,
                TipoOrganizacion.ALIADA);
        when(bodegaService.actualizar(eq(id), any(CuerpoBodega.class))).thenReturn(crearRespuestaBodega());

        mockMvc.perform(put("/api/bodegas/{id}", id)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isOk());
    }

    @Test
    void actualizar_bodegaInexistente_devuelveNotFound() throws Exception {
        var id = UUID.randomUUID();
        var cuerpo = new CuerpoBodega("Bodega Central", "Calle 1 # 2-3", null, null, "900123456-1", null, null,
                TipoOrganizacion.ALIADA);
        when(bodegaService.actualizar(eq(id), any(CuerpoBodega.class)))
                .thenThrow(new BodegaNoEncontradaException(id));

        mockMvc.perform(put("/api/bodegas/{id}", id)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isNotFound());
    }

    // ---------- cambiarEstado ----------

    @Test
    void cambiarEstado_valorValido_devuelveOk() throws Exception {
        var id = UUID.randomUUID();
        when(bodegaService.cambiarEstado(id, EstadoBodega.MANTENIMIENTO)).thenReturn(crearRespuestaBodega());

        mockMvc.perform(patch("/api/bodegas/{id}/estado", id).param("valor", "MANTENIMIENTO"))
                .andExpect(status().isOk());
    }

    @Test
    void cambiarEstado_valorInvalido_devuelveBadRequest() throws Exception {
        var id = UUID.randomUUID();

        mockMvc.perform(patch("/api/bodegas/{id}/estado", id).param("valor", "NO_EXISTE"))
                .andExpect(status().isBadRequest());

        verify(bodegaService, never()).cambiarEstado(any(), any());
    }

    @Test
    void cambiarEstado_bodegaInexistente_devuelveNotFound() throws Exception {
        var id = UUID.randomUUID();
        when(bodegaService.cambiarEstado(id, EstadoBodega.INACTIVA)).thenThrow(new BodegaNoEncontradaException(id));

        mockMvc.perform(patch("/api/bodegas/{id}/estado", id).param("valor", "INACTIVA"))
                .andExpect(status().isNotFound());
    }

    // ---------- helpers ----------

    private RespuestaBodega crearRespuestaBodega() {
        return new RespuestaBodega(UUID.randomUUID(), "Bodega Central", "Calle 1 # 2-3", "3001234567",
                "central@recyops.com", "900123456-1", 4.6, -74.0, "PROPIA", EstadoBodega.ACTIVA,
                LocalDateTime.now());
    }
}
