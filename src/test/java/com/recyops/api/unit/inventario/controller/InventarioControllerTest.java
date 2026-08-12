package com.recyops.api.unit.inventario.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import tools.jackson.databind.ObjectMapper;
import com.recyops.api.comun.dtos.RespuestaPagina;
import com.recyops.api.config.FiltroRateLimit;
import com.recyops.api.inventario.controller.InventarioController;
import com.recyops.api.inventario.dtos.CuerpoAjuste;
import com.recyops.api.inventario.dtos.CuerpoCrearLinea;
import com.recyops.api.inventario.dtos.CuerpoMerma;
import com.recyops.api.inventario.dtos.CuerpoTopes;
import com.recyops.api.inventario.dtos.RespuestaLineaInventario;
import com.recyops.api.inventario.dtos.RespuestaMovimiento;
import com.recyops.api.inventario.enums.TipoOperacion;
import com.recyops.api.inventario.excepciones.LineaDuplicadaException;
import com.recyops.api.inventario.excepciones.LineaInventarioNoEncontradaException;
import com.recyops.api.inventario.excepciones.StockInvalidoException;
import com.recyops.api.inventario.interfaces.InventarioService;
import java.math.BigDecimal;
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
@WebMvcTest(controllers = InventarioController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = FiltroRateLimit.class))
@AutoConfigureMockMvc(addFilters = false)
class InventarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private InventarioService inventarioService;

    // ---------- listar ----------

    @Test
    void listar_bodegaIdDado_devuelveOkConPagina() throws Exception {
        var bodegaId = UUID.randomUUID();
        var respuesta = new RespuestaPagina<>(List.of(crearRespuestaLinea()), 1, 1, 0, 20);
        when(inventarioService.listar(bodegaId, null, null, 0, 20)).thenReturn(respuesta);

        mockMvc.perform(get("/api/inventario").param("bodegaId", bodegaId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void listar_sinBodegaId_devuelveBadRequest() throws Exception {
        mockMvc.perform(get("/api/inventario"))
                .andExpect(status().isBadRequest());

        verify(inventarioService, never()).listar(any(), any(), any(), org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void listar_conFiltros_lospasaAlServicio() throws Exception {
        var bodegaId = UUID.randomUUID();
        var materialId = UUID.randomUUID();
        when(inventarioService.listar(bodegaId, materialId, true, 1, 10))
                .thenReturn(new RespuestaPagina<>(List.of(), 0, 0, 1, 10));

        mockMvc.perform(get("/api/inventario")
                        .param("bodegaId", bodegaId.toString())
                        .param("tipoMaterialId", materialId.toString())
                        .param("bajoMinimo", "true")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk());

        verify(inventarioService).listar(bodegaId, materialId, true, 1, 10);
    }

    @Test
    void listar_pageNegativo_devuelveBadRequest() throws Exception {
        mockMvc.perform(get("/api/inventario")
                        .param("bodegaId", UUID.randomUUID().toString())
                        .param("page", "-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listar_sizeMayorA100_devuelveBadRequest() throws Exception {
        mockMvc.perform(get("/api/inventario")
                        .param("bodegaId", UUID.randomUUID().toString())
                        .param("size", "101"))
                .andExpect(status().isBadRequest());
    }

    // ---------- obtener ----------

    @Test
    void obtener_lineaExistente_devuelveOk() throws Exception {
        var id = UUID.randomUUID();
        when(inventarioService.obtener(id)).thenReturn(crearRespuestaLinea());

        mockMvc.perform(get("/api/inventario/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockActual").value(50));
    }

    @Test
    void obtener_lineaInexistente_devuelveNotFound() throws Exception {
        var id = UUID.randomUUID();
        when(inventarioService.obtener(id)).thenThrow(new LineaInventarioNoEncontradaException(id));

        mockMvc.perform(get("/api/inventario/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // ---------- movimientos ----------

    @Test
    void movimientos_lineaExistente_devuelveOk() throws Exception {
        var id = UUID.randomUUID();
        var respuesta = new RespuestaPagina<>(List.of(crearRespuestaMovimiento()), 1, 1, 0, 20);
        when(inventarioService.movimientos(id, 0, 20)).thenReturn(respuesta);

        mockMvc.perform(get("/api/inventario/{id}/movimientos", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].tipoOperacion").value("ENTRADA"));
    }

    @Test
    void movimientos_pageNegativo_devuelveBadRequest() throws Exception {
        var id = UUID.randomUUID();

        mockMvc.perform(get("/api/inventario/{id}/movimientos", id).param("page", "-1"))
                .andExpect(status().isBadRequest());
    }

    // ---------- crearLinea ----------

    @Test
    void crearLinea_cuerpoValido_devuelveCreated() throws Exception {
        var cuerpo = new CuerpoCrearLinea(UUID.randomUUID(), UUID.randomUUID(), BigDecimal.ZERO,
                BigDecimal.valueOf(100));
        when(inventarioService.crearLinea(any(CuerpoCrearLinea.class))).thenReturn(crearRespuestaLinea());

        mockMvc.perform(post("/api/inventario")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.stockActual").value(50));
    }

    @Test
    void crearLinea_bodegaIdNulo_devuelveBadRequest() throws Exception {
        var cuerpo = new CuerpoCrearLinea(null, UUID.randomUUID(), BigDecimal.ZERO, BigDecimal.valueOf(100));

        mockMvc.perform(post("/api/inventario")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isBadRequest());

        verify(inventarioService, never()).crearLinea(any());
    }

    @Test
    void crearLinea_stockMinimoNegativo_devuelveBadRequest() throws Exception {
        var cuerpo = new CuerpoCrearLinea(UUID.randomUUID(), UUID.randomUUID(), BigDecimal.valueOf(-1),
                BigDecimal.valueOf(100));

        mockMvc.perform(post("/api/inventario")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crearLinea_lineaDuplicada_devuelveConflict() throws Exception {
        var cuerpo = new CuerpoCrearLinea(UUID.randomUUID(), UUID.randomUUID(), BigDecimal.ZERO,
                BigDecimal.valueOf(100));
        when(inventarioService.crearLinea(any(CuerpoCrearLinea.class))).thenThrow(new LineaDuplicadaException());

        mockMvc.perform(post("/api/inventario")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isConflict());
    }

    // ---------- actualizarTopes ----------

    @Test
    void actualizarTopes_cuerpoValido_devuelveOk() throws Exception {
        var id = UUID.randomUUID();
        var cuerpo = new CuerpoTopes(BigDecimal.ZERO, BigDecimal.valueOf(200));
        when(inventarioService.actualizarTopes(eq(id), any(CuerpoTopes.class))).thenReturn(crearRespuestaLinea());

        mockMvc.perform(put("/api/inventario/{id}", id)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isOk());
    }

    @Test
    void actualizarTopes_stockMinimoNulo_devuelveBadRequest() throws Exception {
        var id = UUID.randomUUID();
        var cuerpo = new CuerpoTopes(null, BigDecimal.valueOf(200));

        mockMvc.perform(put("/api/inventario/{id}", id)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isBadRequest());

        verify(inventarioService, never()).actualizarTopes(any(), any());
    }

    @Test
    void actualizarTopes_minimoMayorQueMaximo_devuelveBadRequest() throws Exception {
        var id = UUID.randomUUID();
        var cuerpo = new CuerpoTopes(BigDecimal.valueOf(500), BigDecimal.valueOf(100));
        when(inventarioService.actualizarTopes(eq(id), any(CuerpoTopes.class)))
                .thenThrow(new StockInvalidoException("El stock minimo no puede ser mayor que el stock maximo"));

        mockMvc.perform(put("/api/inventario/{id}", id)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isBadRequest());
    }

    // ---------- registrarAjuste ----------

    @Test
    void registrarAjuste_cuerpoValido_devuelveOk() throws Exception {
        var id = UUID.randomUUID();
        var cuerpo = new CuerpoAjuste(BigDecimal.valueOf(80), "Conteo fisico");
        when(inventarioService.registrarAjuste(eq(id), any(CuerpoAjuste.class))).thenReturn(crearRespuestaLinea());

        mockMvc.perform(post("/api/inventario/{id}/ajuste", id)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isOk());
    }

    @Test
    void registrarAjuste_motivoEnBlanco_devuelveBadRequest() throws Exception {
        var id = UUID.randomUUID();
        var cuerpo = new CuerpoAjuste(BigDecimal.valueOf(80), "");

        mockMvc.perform(post("/api/inventario/{id}/ajuste", id)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isBadRequest());

        verify(inventarioService, never()).registrarAjuste(any(), any());
    }

    @Test
    void registrarAjuste_cantidadNulaOnegativa_devuelveBadRequest() throws Exception {
        var id = UUID.randomUUID();
        var cuerpo = new CuerpoAjuste(BigDecimal.valueOf(-1), "Conteo fisico");

        mockMvc.perform(post("/api/inventario/{id}/ajuste", id)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isBadRequest());
    }

    // ---------- registrarMerma ----------

    @Test
    void registrarMerma_cuerpoValido_devuelveOk() throws Exception {
        var id = UUID.randomUUID();
        var cuerpo = new CuerpoMerma(BigDecimal.TEN, "Material danado");
        when(inventarioService.registrarMerma(eq(id), any(CuerpoMerma.class))).thenReturn(crearRespuestaLinea());

        mockMvc.perform(post("/api/inventario/{id}/merma", id)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isOk());
    }

    @Test
    void registrarMerma_cantidadNoPositiva_devuelveBadRequest() throws Exception {
        var id = UUID.randomUUID();
        var cuerpo = new CuerpoMerma(BigDecimal.ZERO, "Material danado");

        mockMvc.perform(post("/api/inventario/{id}/merma", id)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isBadRequest());

        verify(inventarioService, never()).registrarMerma(any(), any());
    }

    @Test
    void registrarMerma_superaStockActual_devuelveBadRequest() throws Exception {
        var id = UUID.randomUUID();
        var cuerpo = new CuerpoMerma(BigDecimal.valueOf(1000), "Material danado");
        when(inventarioService.registrarMerma(eq(id), any(CuerpoMerma.class)))
                .thenThrow(new StockInvalidoException("La merma supera el stock actual"));

        mockMvc.perform(post("/api/inventario/{id}/merma", id)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isBadRequest());
    }

    // ---------- helpers ----------

    private RespuestaLineaInventario crearRespuestaLinea() {
        return new RespuestaLineaInventario(UUID.randomUUID(), UUID.randomUUID(), "Bodega Central",
                UUID.randomUUID(), "PET Transparente", "Plasticos", "KILOGRAMO", BigDecimal.valueOf(50),
                BigDecimal.ZERO, BigDecimal.valueOf(200), false, LocalDateTime.now());
    }

    private RespuestaMovimiento crearRespuestaMovimiento() {
        return new RespuestaMovimiento(UUID.randomUUID(), TipoOperacion.ENTRADA, BigDecimal.TEN, BigDecimal.ZERO,
                BigDecimal.TEN, "ENT-000001", "Admin Uno", LocalDateTime.now());
    }
}
