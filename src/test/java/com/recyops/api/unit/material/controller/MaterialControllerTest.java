package com.recyops.api.unit.material.controller;

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
import com.recyops.api.material.controller.MaterialController;
import com.recyops.api.material.dtos.CuerpoCambioActivo;
import com.recyops.api.material.dtos.CuerpoMaterial;
import com.recyops.api.material.dtos.RespuestaMaterial;
import com.recyops.api.material.dtos.RespuestaOpcionCatalogo;
import com.recyops.api.material.enums.TipoOpcionCatalogo;
import com.recyops.api.material.enums.UnidadEmpaque;
import com.recyops.api.material.enums.UnidadMedida;
import com.recyops.api.material.excepciones.MaterialNoEncontradoException;
import com.recyops.api.material.excepciones.OpcionCatalogoNoEncontradaException;
import com.recyops.api.material.interfaces.MaterialService;
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
@WebMvcTest(controllers = MaterialController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = FiltroRateLimit.class))
@AutoConfigureMockMvc(addFilters = false)
class MaterialControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MaterialService materialService;

    // ---------- listar ----------

    @Test
    void listar_sinParametros_usaPaginaPorDefecto() throws Exception {
        var respuesta = new RespuestaPagina<>(List.of(crearRespuestaMaterial()), 1, 1, 0, 20);
        when(materialService.listar(null, null, null, null, null, 0, 20)).thenReturn(respuesta);

        mockMvc.perform(get("/api/materiales"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void listar_conFiltros_lospasaAlServicio() throws Exception {
        when(materialService.listar("PLASTICO", "PET", "TRANSPARENTE", "PACA", true, 1, 10))
                .thenReturn(new RespuestaPagina<>(List.of(), 0, 0, 1, 10));

        mockMvc.perform(get("/api/materiales")
                        .param("categoria", "PLASTICO")
                        .param("resina", "PET")
                        .param("color", "TRANSPARENTE")
                        .param("empaque", "PACA")
                        .param("activo", "true")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk());

        verify(materialService).listar("PLASTICO", "PET", "TRANSPARENTE", "PACA", true, 1, 10);
    }

    @Test
    void listar_pageNegativo_devuelveBadRequest() throws Exception {
        mockMvc.perform(get("/api/materiales").param("page", "-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listar_sizeMayorA100_devuelveBadRequest() throws Exception {
        mockMvc.perform(get("/api/materiales").param("size", "101"))
                .andExpect(status().isBadRequest());
    }

    // ---------- categorias / resinas / colores ----------

    @Test
    void categorias_devuelveOk() throws Exception {
        when(materialService.listarCategorias()).thenReturn(List.of(crearRespuestaOpcion()));

        mockMvc.perform(get("/api/materiales/categorias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("PLASTICO"));
    }

    @Test
    void resinas_devuelveOk() throws Exception {
        when(materialService.listarResinas()).thenReturn(List.of(crearRespuestaOpcion()));

        mockMvc.perform(get("/api/materiales/resinas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)));
    }

    @Test
    void colores_devuelveOk() throws Exception {
        when(materialService.listarColores()).thenReturn(List.of(crearRespuestaOpcion()));

        mockMvc.perform(get("/api/materiales/colores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)));
    }

    // ---------- crear ----------

    @Test
    void crear_cuerpoValido_devuelveCreated() throws Exception {
        var cuerpo = crearCuerpoMaterial();
        when(materialService.crear(any(CuerpoMaterial.class))).thenReturn(crearRespuestaMaterial());

        mockMvc.perform(post("/api/materiales")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("PET Transparente"));
    }

    @Test
    void crear_nombreEnBlanco_devuelveBadRequest() throws Exception {
        var cuerpo = new CuerpoMaterial("", "PLASTICO", null, null, UnidadMedida.KILOGRAMO,
                UnidadEmpaque.PACA, BigDecimal.TEN, BigDecimal.ONE, null);

        mockMvc.perform(post("/api/materiales")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isBadRequest());

        verify(materialService, never()).crear(any());
    }

    @Test
    void crear_categoriaCodigoEnBlanco_devuelveBadRequest() throws Exception {
        var cuerpo = new CuerpoMaterial("PET Transparente", "", null, null, UnidadMedida.KILOGRAMO,
                UnidadEmpaque.PACA, BigDecimal.TEN, BigDecimal.ONE, null);

        mockMvc.perform(post("/api/materiales")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crear_precioBaseNoPositivo_devuelveBadRequest() throws Exception {
        var cuerpo = new CuerpoMaterial("PET Transparente", "PLASTICO", null, null, UnidadMedida.KILOGRAMO,
                UnidadEmpaque.PACA, BigDecimal.ZERO, BigDecimal.ONE, null);

        mockMvc.perform(post("/api/materiales")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crear_unidadMedidaNula_devuelveBadRequest() throws Exception {
        var cuerpo = new CuerpoMaterial("PET Transparente", "PLASTICO", null, null, null,
                UnidadEmpaque.PACA, BigDecimal.TEN, BigDecimal.ONE, null);

        mockMvc.perform(post("/api/materiales")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crear_categoriaInexistente_devuelveNotFound() throws Exception {
        var cuerpo = crearCuerpoMaterial();
        when(materialService.crear(any(CuerpoMaterial.class)))
                .thenThrow(new OpcionCatalogoNoEncontradaException(TipoOpcionCatalogo.CATEGORIA, "PLASTICO"));

        mockMvc.perform(post("/api/materiales")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isNotFound());
    }

    // ---------- actualizar ----------

    @Test
    void actualizar_cuerpoValido_devuelveOk() throws Exception {
        var id = UUID.randomUUID();
        var cuerpo = crearCuerpoMaterial();
        when(materialService.actualizar(eq(id), any(CuerpoMaterial.class))).thenReturn(crearRespuestaMaterial());

        mockMvc.perform(put("/api/materiales/{id}", id)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isOk());
    }

    @Test
    void actualizar_materialInexistente_devuelveNotFound() throws Exception {
        var id = UUID.randomUUID();
        var cuerpo = crearCuerpoMaterial();
        when(materialService.actualizar(eq(id), any(CuerpoMaterial.class)))
                .thenThrow(new MaterialNoEncontradoException(id));

        mockMvc.perform(put("/api/materiales/{id}", id)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void actualizar_factorCalidadNulo_devuelveBadRequest() throws Exception {
        var id = UUID.randomUUID();
        var cuerpo = new CuerpoMaterial("PET Transparente", "PLASTICO", null, null, UnidadMedida.KILOGRAMO,
                UnidadEmpaque.PACA, BigDecimal.TEN, null, null);

        mockMvc.perform(put("/api/materiales/{id}", id)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isBadRequest());

        verify(materialService, never()).actualizar(any(), any());
    }

    // ---------- cambiarActivo ----------

    @Test
    void cambiarActivo_cuerpoValido_devuelveOk() throws Exception {
        var id = UUID.randomUUID();
        var cuerpo = new CuerpoCambioActivo(false);
        when(materialService.cambiarActivo(id, false)).thenReturn(crearRespuestaMaterial());

        mockMvc.perform(patch("/api/materiales/{id}", id)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isOk());

        verify(materialService).cambiarActivo(id, false);
    }

    @Test
    void cambiarActivo_activoNulo_devuelveBadRequest() throws Exception {
        var id = UUID.randomUUID();
        var cuerpo = new CuerpoCambioActivo(null);

        mockMvc.perform(patch("/api/materiales/{id}", id)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isBadRequest());

        verify(materialService, never()).cambiarActivo(any(), org.mockito.ArgumentMatchers.anyBoolean());
    }

    @Test
    void cambiarActivo_materialInexistente_devuelveNotFound() throws Exception {
        var id = UUID.randomUUID();
        var cuerpo = new CuerpoCambioActivo(true);
        when(materialService.cambiarActivo(id, true)).thenThrow(new MaterialNoEncontradoException(id));

        mockMvc.perform(patch("/api/materiales/{id}", id)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isNotFound());
    }

    // ---------- helpers ----------

    private CuerpoMaterial crearCuerpoMaterial() {
        return new CuerpoMaterial("PET Transparente", "PLASTICO", "PET", "TRANSPARENTE",
                UnidadMedida.KILOGRAMO, UnidadEmpaque.PACA, BigDecimal.TEN, BigDecimal.ONE, BigDecimal.valueOf(5));
    }

    private RespuestaMaterial crearRespuestaMaterial() {
        return new RespuestaMaterial(UUID.randomUUID(), "PET Transparente", "PLASTICO", "Plasticos",
                "PET", "PET", "TRANSPARENTE", "Transparente", "KILOGRAMO", "PACA", BigDecimal.TEN, BigDecimal.ONE,
                BigDecimal.valueOf(5), true, LocalDateTime.now());
    }

    private RespuestaOpcionCatalogo crearRespuestaOpcion() {
        return new RespuestaOpcionCatalogo("PLASTICO", "Plasticos", false);
    }
}
