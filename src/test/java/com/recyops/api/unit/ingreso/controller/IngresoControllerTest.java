package com.recyops.api.unit.ingreso.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.recyops.api.comun.dtos.RespuestaPagina;
import com.recyops.api.config.FiltroRateLimit;
import com.recyops.api.ingreso.controller.IngresoController;
import com.recyops.api.ingreso.dtos.CuerpoDetalleIngreso;
import com.recyops.api.ingreso.dtos.CuerpoIngreso;
import com.recyops.api.ingreso.dtos.CuerpoPago;
import com.recyops.api.ingreso.dtos.RespuestaIngreso;
import com.recyops.api.ingreso.enums.EstadoIngreso;
import com.recyops.api.ingreso.enums.MetodoPago;
import com.recyops.api.ingreso.excepciones.IngresoNoEncontradoException;
import com.recyops.api.ingreso.interfaces.IngresoService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

/**
 * Slice de MVC: valida mapeo/serializacion, validacion de entrada, traduccion
 * de excepciones y los @PreAuthorize declarados en el propio controlador
 * (historial/obtener/registrarPago/cambiarEstado/cambiarPaso son solo ADMIN;
 * obtenerPorUuid y registrar quedan abiertos a cualquier usuario autenticado,
 * igual que en produccion via SecurityConfig).
 */
@WebMvcTest(controllers = IngresoController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = FiltroRateLimit.class))
@Import(IngresoControllerTest.SeguridadMetodoTestConfig.class)
class IngresoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IngresoService ingresoService;

    // ---------- historial ----------

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void historial_comoAdmin_devuelveOk() throws Exception {
        var pagina = new RespuestaPagina<>(List.of(crearRespuesta(1L)), 1, 1, 0, 20);
        when(ingresoService.historial(eq(null), eq(null), eq(0), eq(20))).thenReturn(pagina);

        mockMvc.perform(get("/api/ingresos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @WithMockUser(authorities = "ROLE_OPERARIO")
    void historial_comoOperario_devuelveForbidden() throws Exception {
        mockMvc.perform(get("/api/ingresos"))
                .andExpect(status().isForbidden());
    }

    // ---------- obtener ----------

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void obtener_idExistente_devuelveOk() throws Exception {
        when(ingresoService.obtener(1L)).thenReturn(crearRespuesta(1L));

        mockMvc.perform(get("/api/ingresos/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void obtener_idInexistente_devuelveNotFound() throws Exception {
        when(ingresoService.obtener(99L)).thenThrow(new IngresoNoEncontradoException(99L));

        mockMvc.perform(get("/api/ingresos/{id}", 99L))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = "ROLE_OPERARIO")
    void obtener_comoOperario_devuelveForbidden() throws Exception {
        mockMvc.perform(get("/api/ingresos/{id}", 1L))
                .andExpect(status().isForbidden());
    }

    // ---------- obtenerPorUuid ----------

    @Test
    @WithMockUser(authorities = "ROLE_OPERARIO")
    void obtenerPorUuid_comoOperario_devuelveOk() throws Exception {
        var uuid = UUID.randomUUID();
        when(ingresoService.obtenerPorUuid(uuid)).thenReturn(crearRespuesta(1L));

        mockMvc.perform(get("/api/ingresos/uuid/{uuid}", uuid))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "ROLE_OPERARIO")
    void obtenerPorUuid_uuidInvalido_devuelveBadRequest() throws Exception {
        mockMvc.perform(get("/api/ingresos/uuid/{uuid}", "no-es-un-uuid"))
                .andExpect(status().isBadRequest());
    }

    // ---------- registrar ----------

    @Test
    @WithMockUser(authorities = "ROLE_OPERARIO")
    void registrar_cuerpoValido_devuelveCreated() throws Exception {
        var cuerpo = new CuerpoIngreso("Cliente Uno", "123456789", UUID.randomUUID(), "Encargado",
                "ABC123", BigDecimal.TEN, BigDecimal.valueOf(50000), List.of(detalleValido()));
        when(ingresoService.registrar(any(CuerpoIngreso.class))).thenReturn(crearRespuesta(1L));

        mockMvc.perform(post("/api/ingresos")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(authorities = "ROLE_OPERARIO")
    void registrar_clienteEnBlanco_devuelveBadRequest() throws Exception {
        var cuerpo = new CuerpoIngreso("", "123456789", UUID.randomUUID(), "Encargado",
                null, BigDecimal.TEN, BigDecimal.valueOf(50000), List.of(detalleValido()));

        mockMvc.perform(post("/api/ingresos")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isBadRequest());

        verify(ingresoService, never()).registrar(any());
    }

    @Test
    @WithMockUser(authorities = "ROLE_OPERARIO")
    void registrar_pesoNoPositivo_devuelveBadRequest() throws Exception {
        var cuerpo = new CuerpoIngreso("Cliente Uno", "123456789", UUID.randomUUID(), "Encargado",
                null, BigDecimal.ZERO, BigDecimal.valueOf(50000), List.of(detalleValido()));

        mockMvc.perform(post("/api/ingresos")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "ROLE_OPERARIO")
    void registrar_bodegaDestinoIdNulo_devuelveBadRequest() throws Exception {
        var cuerpo = new CuerpoIngreso("Cliente Uno", "123456789", null, "Encargado",
                null, BigDecimal.TEN, BigDecimal.valueOf(50000), List.of(detalleValido()));

        mockMvc.perform(post("/api/ingresos")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isBadRequest());

        verify(ingresoService, never()).registrar(any());
    }

    @Test
    @WithMockUser(authorities = "ROLE_OPERARIO")
    void registrar_materialesVacio_devuelveBadRequest() throws Exception {
        var cuerpo = new CuerpoIngreso("Cliente Uno", "123456789", UUID.randomUUID(), "Encargado",
                null, BigDecimal.TEN, BigDecimal.valueOf(50000), List.of());

        mockMvc.perform(post("/api/ingresos")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isBadRequest());

        verify(ingresoService, never()).registrar(any());
    }

    @Test
    @WithMockUser(authorities = "ROLE_OPERARIO")
    void registrar_detalleSinMaterialId_devuelveBadRequest() throws Exception {
        var detalleSinMaterial = new CuerpoDetalleIngreso(null, BigDecimal.TEN, BigDecimal.ZERO,
                BigDecimal.valueOf(1000), null);
        var cuerpo = new CuerpoIngreso("Cliente Uno", "123456789", UUID.randomUUID(), "Encargado",
                null, BigDecimal.TEN, BigDecimal.valueOf(50000), List.of(detalleSinMaterial));

        mockMvc.perform(post("/api/ingresos")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isBadRequest());

        verify(ingresoService, never()).registrar(any());
    }

    // ---------- registrarPago ----------

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void registrarPago_cuerpoValido_devuelveOk() throws Exception {
        var cuerpo = new CuerpoPago(MetodoPago.EFECTIVO);
        when(ingresoService.registrarPago(eq(1L), any(CuerpoPago.class))).thenReturn(crearRespuesta(1L));

        mockMvc.perform(patch("/api/ingresos/{id}/pago", 1L)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void registrarPago_metodoNulo_devuelveBadRequest() throws Exception {
        mockMvc.perform(patch("/api/ingresos/{id}/pago", 1L)
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "ROLE_OPERARIO")
    void registrarPago_comoOperario_devuelveForbidden() throws Exception {
        var cuerpo = new CuerpoPago(MetodoPago.EFECTIVO);

        mockMvc.perform(patch("/api/ingresos/{id}/pago", 1L)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isForbidden());
    }

    // ---------- cambiarEstado ----------

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void cambiarEstado_valorValido_devuelveOk() throws Exception {
        when(ingresoService.cambiarEstado(1L, EstadoIngreso.EN_BODEGA)).thenReturn(crearRespuesta(1L));

        mockMvc.perform(patch("/api/ingresos/{id}/estado", 1L).param("valor", "EN_BODEGA"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void cambiarEstado_valorInvalido_devuelveBadRequest() throws Exception {
        mockMvc.perform(patch("/api/ingresos/{id}/estado", 1L).param("valor", "NO_EXISTE"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "ROLE_OPERARIO")
    void cambiarEstado_comoOperario_devuelveForbidden() throws Exception {
        mockMvc.perform(patch("/api/ingresos/{id}/estado", 1L).param("valor", "EN_BODEGA"))
                .andExpect(status().isForbidden());
    }

    // ---------- cambiarPaso ----------

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void cambiarPaso_valorValido_devuelveOk() throws Exception {
        when(ingresoService.cambiarPaso(1L, true)).thenReturn(crearRespuesta(1L));

        mockMvc.perform(patch("/api/ingresos/{id}/paso", 1L).param("valor", "true"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "ROLE_OPERARIO")
    void cambiarPaso_comoOperario_devuelveForbidden() throws Exception {
        mockMvc.perform(patch("/api/ingresos/{id}/paso", 1L).param("valor", "true"))
                .andExpect(status().isForbidden());
    }

    // ---------- helpers ----------

    private CuerpoDetalleIngreso detalleValido() {
        return new CuerpoDetalleIngreso(UUID.randomUUID(), BigDecimal.TEN, BigDecimal.ZERO,
                BigDecimal.valueOf(1000), null);
    }

    private RespuestaIngreso crearRespuesta(Long id) {
        return new RespuestaIngreso(id, UUID.randomUUID(), LocalDateTime.now(), "Cliente Uno", "123456789",
                "Bodega Norte", "Encargado", "ABC123", BigDecimal.TEN, BigDecimal.valueOf(50000),
                "POR_CLASIFICAR", "PENDIENTE", null, false, List.of());
    }

    @EnableMethodSecurity
    static class SeguridadMetodoTestConfig {

        @Bean
        SecurityFilterChain filtroSeguridadTest(HttpSecurity http) throws Exception {
            http.csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(auth -> auth.anyRequest().authenticated());
            return http.build();
        }
    }
}
