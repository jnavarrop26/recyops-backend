package com.recyops.api.unit.tarea.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import tools.jackson.databind.ObjectMapper;
import com.recyops.api.comun.dtos.RespuestaPagina;
import com.recyops.api.comun.excepciones.ReglaNegocioException;
import com.recyops.api.config.FiltroRateLimit;
import com.recyops.api.tarea.controller.TareaController;
import com.recyops.api.tarea.dtos.CuerpoAvance;
import com.recyops.api.tarea.dtos.CuerpoTarea;
import com.recyops.api.tarea.dtos.RespuestaAvance;
import com.recyops.api.tarea.dtos.RespuestaTarea;
import com.recyops.api.tarea.enums.EstadoTarea;
import com.recyops.api.tarea.enums.PrioridadTarea;
import com.recyops.api.tarea.excepciones.TareaAjenaException;
import com.recyops.api.tarea.excepciones.TareaNoEncontradaException;
import com.recyops.api.tarea.excepciones.TransicionTareaInvalidaException;
import com.recyops.api.tarea.interfaces.TareaService;
import java.math.BigDecimal;
import java.time.LocalDate;
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

/**
 * Slice de MVC puro: valida mapeo de rutas, (de)serializacion, validacion de
 * entrada y la traduccion de excepciones vía {@link com.recyops.api.comun.excepciones.ManejadorGlobalExcepciones}.
 * El filtro de seguridad real (JWT + reglas por URL de SecurityConfig) no se
 * carga en este slice; solo se habilita seguridad de metodo para verificar
 * los {@code @PreAuthorize} declarados en el propio controlador.
 */
@WebMvcTest(controllers = TareaController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = FiltroRateLimit.class))
@Import(TareaControllerTest.SeguridadMetodoTestConfig.class)
class TareaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TareaService tareaService;

    // ---------- listar ----------

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void listar_comoAdmin_devuelveOkConPagina() throws Exception {
        var respuesta = new RespuestaPagina<>(List.of(crearRespuestaTarea()), 1, 1, 0, 20);
        when(tareaService.listar(eq(EstadoTarea.PENDIENTE), any(), any(), eq(0), eq(20))).thenReturn(respuesta);

        mockMvc.perform(get("/api/tareas").param("estado", "PENDIENTE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @WithMockUser(authorities = "ROLE_OPERARIO")
    void listar_comoOperario_devuelveForbidden() throws Exception {
        mockMvc.perform(get("/api/tareas"))
                .andExpect(status().isForbidden());

        verify(tareaService, never()).listar(any(), any(), any(), anyInt(), anyInt());
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void listar_pageNegativo_devuelveBadRequest() throws Exception {
        mockMvc.perform(get("/api/tareas").param("page", "-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void listar_sizeMayorA100_devuelveBadRequest() throws Exception {
        mockMvc.perform(get("/api/tareas").param("size", "101"))
                .andExpect(status().isBadRequest());
    }

    // ---------- misTareas ----------

    @Test
    @WithMockUser(authorities = "ROLE_OPERARIO")
    void misTareas_usuarioAutenticado_devuelveOk() throws Exception {
        when(tareaService.misTareas()).thenReturn(List.of(crearRespuestaTarea()));

        mockMvc.perform(get("/api/tareas/mias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].titulo").value("Recoger PET"));
    }

    // ---------- crear ----------

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void crear_cuerpoValido_devuelveCreated() throws Exception {
        var cuerpo = new CuerpoTarea("Recoger PET", "Descripcion", UUID.randomUUID(), null,
                PrioridadTarea.ALTA, LocalDate.of(2026, 3, 1));
        when(tareaService.crear(any(CuerpoTarea.class))).thenReturn(crearRespuestaTarea());

        mockMvc.perform(post("/api/tareas")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.titulo").value("Recoger PET"));
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void crear_tituloEnBlanco_devuelveBadRequest() throws Exception {
        var cuerpo = new CuerpoTarea("", "Descripcion", UUID.randomUUID(), null, PrioridadTarea.ALTA, null);

        mockMvc.perform(post("/api/tareas")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje", containsString("titulo")));

        verify(tareaService, never()).crear(any());
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void crear_asignadoIdNulo_devuelveBadRequest() throws Exception {
        var cuerpo = new CuerpoTarea("Titulo", "Descripcion", null, null, PrioridadTarea.ALTA, null);

        mockMvc.perform(post("/api/tareas")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "ROLE_OPERARIO")
    void crear_comoOperario_devuelveForbidden() throws Exception {
        var cuerpo = new CuerpoTarea("Titulo", "Descripcion", UUID.randomUUID(), null, PrioridadTarea.ALTA, null);

        mockMvc.perform(post("/api/tareas")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isForbidden());
    }

    // ---------- editar ----------

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void editar_cuerpoValido_devuelveOk() throws Exception {
        var id = UUID.randomUUID();
        var cuerpo = new CuerpoTarea("Nuevo titulo", "Descripcion", UUID.randomUUID(), null,
                PrioridadTarea.MEDIA, null);
        when(tareaService.editar(eq(id), any(CuerpoTarea.class))).thenReturn(crearRespuestaTarea());

        mockMvc.perform(put("/api/tareas/{id}", id)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void editar_tareaInexistente_devuelveNotFound() throws Exception {
        var id = UUID.randomUUID();
        var cuerpo = new CuerpoTarea("Titulo", "Descripcion", UUID.randomUUID(), null, PrioridadTarea.MEDIA, null);
        when(tareaService.editar(eq(id), any(CuerpoTarea.class))).thenThrow(new TareaNoEncontradaException(id));

        mockMvc.perform(put("/api/tareas/{id}", id)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // ---------- cambiarEstado ----------

    @Test
    @WithMockUser(authorities = "ROLE_OPERARIO")
    void cambiarEstado_valorValido_devuelveOk() throws Exception {
        var id = UUID.randomUUID();
        when(tareaService.cambiarEstado(id, EstadoTarea.EN_PROGRESO)).thenReturn(crearRespuestaTarea());

        mockMvc.perform(patch("/api/tareas/{id}/estado", id).param("valor", "EN_PROGRESO"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "ROLE_OPERARIO")
    void cambiarEstado_tareaAjena_devuelveForbidden() throws Exception {
        var id = UUID.randomUUID();
        when(tareaService.cambiarEstado(id, EstadoTarea.EN_PROGRESO)).thenThrow(new TareaAjenaException());

        mockMvc.perform(patch("/api/tareas/{id}/estado", id).param("valor", "EN_PROGRESO"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "ROLE_OPERARIO")
    void cambiarEstado_transicionInvalida_devuelveBadRequest() throws Exception {
        var id = UUID.randomUUID();
        when(tareaService.cambiarEstado(id, EstadoTarea.COMPLETADA))
                .thenThrow(new TransicionTareaInvalidaException(EstadoTarea.PENDIENTE, EstadoTarea.COMPLETADA));

        mockMvc.perform(patch("/api/tareas/{id}/estado", id).param("valor", "COMPLETADA"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "ROLE_OPERARIO")
    void cambiarEstado_valorInvalido_devuelveBadRequest() throws Exception {
        var id = UUID.randomUUID();

        mockMvc.perform(patch("/api/tareas/{id}/estado", id).param("valor", "NO_EXISTE"))
                .andExpect(status().isBadRequest());
    }

    // ---------- avances ----------

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void avances_tareaExistente_devuelveOk() throws Exception {
        var id = UUID.randomUUID();
        when(tareaService.listarAvances(id)).thenReturn(List.of(crearRespuestaAvance()));

        mockMvc.perform(get("/api/tareas/{id}/avances", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].descripcion").value("Se cargo el camion"));
    }

    @Test
    @WithMockUser(authorities = "ROLE_OPERARIO")
    void avances_tareaAjena_devuelveForbidden() throws Exception {
        var id = UUID.randomUUID();
        when(tareaService.listarAvances(id)).thenThrow(new TareaAjenaException());

        mockMvc.perform(get("/api/tareas/{id}/avances", id))
                .andExpect(status().isForbidden());
    }

    // ---------- agregarAvance ----------

    @Test
    @WithMockUser(authorities = "ROLE_OPERARIO")
    void agregarAvance_cuerpoValido_devuelveCreated() throws Exception {
        var id = UUID.randomUUID();
        var cuerpo = new CuerpoAvance(BigDecimal.TEN, "Avance parcial");
        when(tareaService.agregarAvance(eq(id), any(CuerpoAvance.class))).thenReturn(crearRespuestaAvance());

        mockMvc.perform(post("/api/tareas/{id}/avances", id)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(authorities = "ROLE_OPERARIO")
    void agregarAvance_cantidadNoPositiva_devuelveBadRequest() throws Exception {
        var id = UUID.randomUUID();
        var cuerpo = new CuerpoAvance(BigDecimal.ZERO, "Avance parcial");

        mockMvc.perform(post("/api/tareas/{id}/avances", id)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isBadRequest());

        verify(tareaService, never()).agregarAvance(any(), any());
    }

    @Test
    @WithMockUser(authorities = "ROLE_OPERARIO")
    void agregarAvance_tareaNoEnProgreso_devuelveBadRequest() throws Exception {
        var id = UUID.randomUUID();
        var cuerpo = new CuerpoAvance(BigDecimal.ONE, "Avance parcial");
        when(tareaService.agregarAvance(eq(id), any(CuerpoAvance.class)))
                .thenThrow(new ReglaNegocioException("La tarea debe estar EN_PROGRESO"));

        mockMvc.perform(post("/api/tareas/{id}/avances", id)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isBadRequest());
    }

    // ---------- eliminar ----------

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void eliminar_tareaEnRevision_devuelveNoContent() throws Exception {
        var id = UUID.randomUUID();

        mockMvc.perform(delete("/api/tareas/{id}", id))
                .andExpect(status().isNoContent());

        verify(tareaService).eliminar(id);
    }

    @Test
    @WithMockUser(authorities = "ROLE_OPERARIO")
    void eliminar_comoOperario_devuelveForbidden() throws Exception {
        var id = UUID.randomUUID();

        mockMvc.perform(delete("/api/tareas/{id}", id))
                .andExpect(status().isForbidden());

        verify(tareaService, never()).eliminar(any());
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void eliminar_tareaNoEnRevision_devuelveBadRequest() throws Exception {
        var id = UUID.randomUUID();
        org.mockito.Mockito.doThrow(new ReglaNegocioException("Solo se pueden eliminar tareas en REVISION"))
                .when(tareaService).eliminar(id);

        mockMvc.perform(delete("/api/tareas/{id}", id))
                .andExpect(status().isBadRequest());
    }

    // ---------- helpers ----------

    private RespuestaTarea crearRespuestaTarea() {
        return new RespuestaTarea(UUID.randomUUID(), "Recoger PET", "Descripcion", UUID.randomUUID(),
                "Maria Lopez", null, null, PrioridadTarea.ALTA, EstadoTarea.PENDIENTE,
                LocalDate.of(2026, 3, 1), false, "Admin Uno", LocalDateTime.now(), null);
    }

    private RespuestaAvance crearRespuestaAvance() {
        return new RespuestaAvance(UUID.randomUUID(), BigDecimal.TEN, "Se cargo el camion", "Operario",
                LocalDateTime.now());
    }

    /**
     * Habilita solo la seguridad de metodo (@PreAuthorize) para este slice.
     * CSRF va deshabilitado igual que en SecurityConfig: la API es un
     * recurso OAuth2 stateless, nunca navegador+cookies de sesion.
     */
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
