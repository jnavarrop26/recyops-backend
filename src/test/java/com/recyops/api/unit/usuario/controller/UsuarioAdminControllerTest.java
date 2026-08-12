package com.recyops.api.unit.usuario.controller;

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

import com.recyops.api.comun.dtos.RespuestaPagina;
import com.recyops.api.config.FiltroRateLimit;
import com.recyops.api.usuario.controller.UsuarioAdminController;
import com.recyops.api.usuario.dtos.CuerpoEditarTrabajador;
import com.recyops.api.usuario.dtos.CuerpoTrabajador;
import com.recyops.api.usuario.dtos.RespuestaTrabajador;
import com.recyops.api.usuario.dtos.RespuestaTrabajadorCreado;
import com.recyops.api.usuario.enums.EstadoUsuario;
import com.recyops.api.usuario.excepciones.RolNoEncontradoException;
import com.recyops.api.usuario.excepciones.UsuarioDuplicadoException;
import com.recyops.api.usuario.excepciones.UsuarioNoEncontradoException;
import com.recyops.api.usuario.interfaces.UsuarioService;
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
import tools.jackson.databind.ObjectMapper;

/**
 * Slice de MVC puro: sin @PreAuthorize (la proteccion ADMIN vive en
 * SecurityConfig como regla por URL, fuera de este slice).
 */
@WebMvcTest(controllers = UsuarioAdminController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = FiltroRateLimit.class))
@AutoConfigureMockMvc(addFilters = false)
class UsuarioAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UsuarioService usuarioService;

    // ---------- listar ----------

    @Test
    void listar_conResultados_devuelveOk() throws Exception {
        var pagina = new RespuestaPagina<>(List.of(crearRespuesta()), 1, 1, 0, 20);
        when(usuarioService.listarTrabajadores(0, 20)).thenReturn(pagina);

        mockMvc.perform(get("/api/admin/usuarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void listar_pageNegativo_devuelveBadRequest() throws Exception {
        mockMvc.perform(get("/api/admin/usuarios").param("page", "-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listar_sizeMayorA100_devuelveBadRequest() throws Exception {
        mockMvc.perform(get("/api/admin/usuarios").param("size", "150"))
                .andExpect(status().isBadRequest());
    }

    // ---------- registrar ----------

    @Test
    void registrar_cuerpoValido_devuelveCreated() throws Exception {
        var cuerpo = new CuerpoTrabajador("Juan Perez", "juan.perez", "juan@test.com", null,
                UUID.randomUUID(), UUID.randomUUID(), null);
        var creado = new RespuestaTrabajadorCreado(UUID.randomUUID(), "juan.perez", "juan@test.com",
                "ACTIVO", "temporal123");
        when(usuarioService.registrarTrabajador(any(CuerpoTrabajador.class))).thenReturn(creado);

        mockMvc.perform(post("/api/admin/usuarios")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("juan.perez"));
    }

    @Test
    void registrar_nombreEnBlanco_devuelveBadRequest() throws Exception {
        var cuerpo = new CuerpoTrabajador("", "juan.perez", "juan@test.com", null,
                UUID.randomUUID(), UUID.randomUUID(), null);

        mockMvc.perform(post("/api/admin/usuarios")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isBadRequest());

        verify(usuarioService, never()).registrarTrabajador(any());
    }

    @Test
    void registrar_emailInvalido_devuelveBadRequest() throws Exception {
        var cuerpo = new CuerpoTrabajador("Juan Perez", "juan.perez", "no-es-un-email", null,
                UUID.randomUUID(), UUID.randomUUID(), null);

        mockMvc.perform(post("/api/admin/usuarios")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registrar_passwordCorta_devuelveBadRequest() throws Exception {
        var cuerpo = new CuerpoTrabajador("Juan Perez", "juan.perez", "juan@test.com", null,
                UUID.randomUUID(), UUID.randomUUID(), "abc123");

        mockMvc.perform(post("/api/admin/usuarios")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registrar_usernameDuplicado_devuelveConflict() throws Exception {
        var cuerpo = new CuerpoTrabajador("Juan Perez", "juan.perez", "juan@test.com", null,
                UUID.randomUUID(), UUID.randomUUID(), null);
        when(usuarioService.registrarTrabajador(any(CuerpoTrabajador.class)))
                .thenThrow(new UsuarioDuplicadoException("username", "juan.perez"));

        mockMvc.perform(post("/api/admin/usuarios")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isConflict());
    }

    @Test
    void registrar_rolInexistente_devuelveNotFound() throws Exception {
        var rolId = UUID.randomUUID();
        var cuerpo = new CuerpoTrabajador("Juan Perez", "juan.perez", "juan@test.com", null,
                UUID.randomUUID(), rolId, null);
        when(usuarioService.registrarTrabajador(any(CuerpoTrabajador.class)))
                .thenThrow(new RolNoEncontradoException(rolId));

        mockMvc.perform(post("/api/admin/usuarios")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isNotFound());
    }

    // ---------- editar ----------

    @Test
    void editar_cuerpoValido_devuelveOk() throws Exception {
        var id = UUID.randomUUID();
        var cuerpo = new CuerpoEditarTrabajador("Juan Perez Editado", null, UUID.randomUUID(), UUID.randomUUID());
        when(usuarioService.editarTrabajador(eq(id), any(CuerpoEditarTrabajador.class)))
                .thenReturn(crearRespuesta());

        mockMvc.perform(put("/api/admin/usuarios/{id}", id)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isOk());
    }

    @Test
    void editar_usuarioInexistente_devuelveNotFound() throws Exception {
        var id = UUID.randomUUID();
        var cuerpo = new CuerpoEditarTrabajador("Juan Perez", null, UUID.randomUUID(), UUID.randomUUID());
        when(usuarioService.editarTrabajador(eq(id), any(CuerpoEditarTrabajador.class)))
                .thenThrow(new UsuarioNoEncontradoException(id));

        mockMvc.perform(put("/api/admin/usuarios/{id}", id)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isNotFound());
    }

    @Test
    void editar_bodegaIdNulo_devuelveBadRequest() throws Exception {
        var id = UUID.randomUUID();
        var cuerpo = new CuerpoEditarTrabajador("Juan Perez", null, null, UUID.randomUUID());

        mockMvc.perform(put("/api/admin/usuarios/{id}", id)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isBadRequest());
    }

    // ---------- cambiarEstado ----------

    @Test
    void cambiarEstado_valorValido_devuelveOk() throws Exception {
        var id = UUID.randomUUID();
        when(usuarioService.cambiarEstado(id, EstadoUsuario.INACTIVO)).thenReturn(crearRespuesta());

        mockMvc.perform(patch("/api/admin/usuarios/{id}/estado", id).param("valor", "INACTIVO"))
                .andExpect(status().isOk());
    }

    @Test
    void cambiarEstado_valorInvalido_devuelveBadRequest() throws Exception {
        var id = UUID.randomUUID();

        mockMvc.perform(patch("/api/admin/usuarios/{id}/estado", id).param("valor", "NO_EXISTE"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cambiarEstado_usuarioInexistente_devuelveNotFound() throws Exception {
        var id = UUID.randomUUID();
        when(usuarioService.cambiarEstado(id, EstadoUsuario.ACTIVO))
                .thenThrow(new UsuarioNoEncontradoException(id));

        mockMvc.perform(patch("/api/admin/usuarios/{id}/estado", id).param("valor", "ACTIVO"))
                .andExpect(status().isNotFound());
    }

    // ---------- helpers ----------

    private RespuestaTrabajador crearRespuesta() {
        return new RespuestaTrabajador(UUID.randomUUID(), "Juan Perez", "juan.perez", "juan@test.com", null,
                "ACTIVO", UUID.randomUUID(), "OPERARIO", UUID.randomUUID(), "Bodega Norte");
    }
}
