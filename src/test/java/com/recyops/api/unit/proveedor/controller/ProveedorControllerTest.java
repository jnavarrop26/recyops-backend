package com.recyops.api.unit.proveedor.controller;

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
import com.recyops.api.proveedor.controller.ProveedorController;
import com.recyops.api.proveedor.dtos.CuerpoProveedor;
import com.recyops.api.proveedor.dtos.RespuestaEntregaProveedor;
import com.recyops.api.proveedor.dtos.RespuestaProveedor;
import com.recyops.api.proveedor.enums.EstadoProveedor;
import com.recyops.api.proveedor.excepciones.CalificacionInvalidaException;
import com.recyops.api.proveedor.excepciones.ProveedorNoEncontradoException;
import com.recyops.api.proveedor.interfaces.ProveedorService;
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
@WebMvcTest(controllers = ProveedorController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = FiltroRateLimit.class))
@AutoConfigureMockMvc(addFilters = false)
class ProveedorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProveedorService proveedorService;

    // ---------- listar ----------

    @Test
    void listar_sinParametros_devuelveOkConPagina() throws Exception {
        var respuesta = new RespuestaPagina<>(List.of(crearRespuestaProveedor()), 1, 1, 0, 20);
        when(proveedorService.listar(null, null, null, 0, 20)).thenReturn(respuesta);

        mockMvc.perform(get("/api/proveedores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void listar_conFiltros_lospasaAlServicio() throws Exception {
        when(proveedorService.listar(EstadoProveedor.ACTIVO, "Reciclajes SA", BigDecimal.valueOf(3.5), 0, 20))
                .thenReturn(new RespuestaPagina<>(List.of(), 0, 0, 0, 20));

        mockMvc.perform(get("/api/proveedores")
                        .param("estado", "ACTIVO")
                        .param("nombre", "Reciclajes SA")
                        .param("calificacionMin", "3.5"))
                .andExpect(status().isOk());

        verify(proveedorService).listar(EstadoProveedor.ACTIVO, "Reciclajes SA", BigDecimal.valueOf(3.5), 0, 20);
    }

    @Test
    void listar_pageNegativo_devuelveBadRequest() throws Exception {
        mockMvc.perform(get("/api/proveedores").param("page", "-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listar_sizeMayorA100_devuelveBadRequest() throws Exception {
        mockMvc.perform(get("/api/proveedores").param("size", "101"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listar_estadoInvalido_devuelveBadRequest() throws Exception {
        mockMvc.perform(get("/api/proveedores").param("estado", "NO_EXISTE"))
                .andExpect(status().isBadRequest());
    }

    // ---------- crear ----------

    @Test
    void crear_cuerpoValido_devuelveCreated() throws Exception {
        var cuerpo = new CuerpoProveedor("Reciclajes SA", "900123456-1", "Juan Perez", "3001234567",
                "contacto@reciclajes.com", "Calle 1 # 2-3");
        when(proveedorService.crear(any(CuerpoProveedor.class))).thenReturn(crearRespuestaProveedor());

        mockMvc.perform(post("/api/proveedores")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Reciclajes SA"));
    }

    @Test
    void crear_nombreEnBlanco_devuelveBadRequest() throws Exception {
        var cuerpo = new CuerpoProveedor("", "900123456-1", null, null, null, null);

        mockMvc.perform(post("/api/proveedores")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isBadRequest());

        verify(proveedorService, never()).crear(any());
    }

    @Test
    void crear_nitEnBlanco_devuelveBadRequest() throws Exception {
        var cuerpo = new CuerpoProveedor("Reciclajes SA", "", null, null, null, null);

        mockMvc.perform(post("/api/proveedores")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isBadRequest());

        verify(proveedorService, never()).crear(any());
    }

    @Test
    void crear_emailInvalido_devuelveBadRequest() throws Exception {
        var cuerpo = new CuerpoProveedor("Reciclajes SA", "900123456-1", null, null, "no-es-un-correo", null);

        mockMvc.perform(post("/api/proveedores")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isBadRequest());

        verify(proveedorService, never()).crear(any());
    }

    // ---------- actualizar ----------

    @Test
    void actualizar_cuerpoValido_devuelveOk() throws Exception {
        var id = UUID.randomUUID();
        var cuerpo = new CuerpoProveedor("Reciclajes SA", "900123456-1", null, null, null, null);
        when(proveedorService.actualizar(eq(id), any(CuerpoProveedor.class))).thenReturn(crearRespuestaProveedor());

        mockMvc.perform(put("/api/proveedores/{id}", id)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isOk());
    }

    @Test
    void actualizar_proveedorInexistente_devuelveNotFound() throws Exception {
        var id = UUID.randomUUID();
        var cuerpo = new CuerpoProveedor("Reciclajes SA", "900123456-1", null, null, null, null);
        when(proveedorService.actualizar(eq(id), any(CuerpoProveedor.class)))
                .thenThrow(new ProveedorNoEncontradoException(id));

        mockMvc.perform(put("/api/proveedores/{id}", id)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isNotFound());
    }

    // ---------- cambiarEstado ----------

    @Test
    void cambiarEstado_valorValido_devuelveOk() throws Exception {
        var id = UUID.randomUUID();
        when(proveedorService.cambiarEstado(id, EstadoProveedor.BLOQUEADO)).thenReturn(crearRespuestaProveedor());

        mockMvc.perform(patch("/api/proveedores/{id}/estado", id).param("valor", "BLOQUEADO"))
                .andExpect(status().isOk());
    }

    @Test
    void cambiarEstado_valorInvalido_devuelveBadRequest() throws Exception {
        var id = UUID.randomUUID();

        mockMvc.perform(patch("/api/proveedores/{id}/estado", id).param("valor", "NO_EXISTE"))
                .andExpect(status().isBadRequest());

        verify(proveedorService, never()).cambiarEstado(any(), any());
    }

    @Test
    void cambiarEstado_proveedorInexistente_devuelveNotFound() throws Exception {
        var id = UUID.randomUUID();
        when(proveedorService.cambiarEstado(id, EstadoProveedor.INACTIVO))
                .thenThrow(new ProveedorNoEncontradoException(id));

        mockMvc.perform(patch("/api/proveedores/{id}/estado", id).param("valor", "INACTIVO"))
                .andExpect(status().isNotFound());
    }

    // ---------- calificar ----------

    @Test
    void calificar_valorValido_devuelveOk() throws Exception {
        var id = UUID.randomUUID();
        when(proveedorService.calificar(id, 4.5)).thenReturn(crearRespuestaProveedor());

        mockMvc.perform(patch("/api/proveedores/{id}/calificacion", id).param("valor", "4.5"))
                .andExpect(status().isOk());
    }

    @Test
    void calificar_valorFueraDeRango_devuelveBadRequest() throws Exception {
        var id = UUID.randomUUID();
        when(proveedorService.calificar(id, 7.0)).thenThrow(new CalificacionInvalidaException(7.0));

        mockMvc.perform(patch("/api/proveedores/{id}/calificacion", id).param("valor", "7.0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void calificar_valorNoNumerico_devuelveBadRequest() throws Exception {
        var id = UUID.randomUUID();

        mockMvc.perform(patch("/api/proveedores/{id}/calificacion", id).param("valor", "no-numero"))
                .andExpect(status().isBadRequest());

        verify(proveedorService, never()).calificar(any(), org.mockito.ArgumentMatchers.anyDouble());
    }

    // ---------- entregas ----------

    @Test
    void entregas_proveedorExistente_devuelveOk() throws Exception {
        var id = UUID.randomUUID();
        var entrega = new RespuestaEntregaProveedor(UUID.randomUUID(), "ENT-001", "PET", BigDecimal.TEN, "RECIBIDA",
                LocalDateTime.now());
        when(proveedorService.entregas(id)).thenReturn(List.of(entrega));

        mockMvc.perform(get("/api/proveedores/{id}/entregas", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("ENT-001"));
    }

    // ---------- helpers ----------

    private RespuestaProveedor crearRespuestaProveedor() {
        return new RespuestaProveedor(UUID.randomUUID(), "Reciclajes SA", "900123456-1", "Juan Perez",
                "3001234567", "contacto@reciclajes.com", "Calle 1 # 2-3", BigDecimal.valueOf(4.5),
                EstadoProveedor.ACTIVO, LocalDateTime.now());
    }
}
