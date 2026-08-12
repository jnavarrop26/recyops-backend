package com.recyops.api.unit.platform.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.recyops.api.config.FiltroRateLimit;
import com.recyops.api.platform.controller.PlataformaController;
import com.recyops.api.platform.dtos.CuerpoNuevaEmpresa;
import com.recyops.api.platform.dtos.RespuestaEmpresaCreada;
import com.recyops.api.platform.excepciones.EmpresaYaExisteException;
import com.recyops.api.platform.excepciones.SchemaInvalidoException;
import com.recyops.api.platform.interfaces.PlataformaService;
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
 * Slice de MVC: el @PreAuthorize("hasRole('SUPERADMIN')") esta a nivel de
 * clase, asi que todo el controlador debe rechazar cualquier otro rol.
 */
@WebMvcTest(controllers = PlataformaController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = FiltroRateLimit.class))
@Import(PlataformaControllerTest.SeguridadMetodoTestConfig.class)
class PlataformaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PlataformaService plataformaService;

    @Test
    @WithMockUser(authorities = "ROLE_SUPERADMIN")
    void provisionarEmpresa_cuerpoValido_devuelveCreated() throws Exception {
        var cuerpo = new CuerpoNuevaEmpresa("Empresa Demo", "900123456-1", "empresa_demo",
                "admin@demo.com", "Admin Demo", "admin.demo", null);
        var respuesta = new RespuestaEmpresaCreada(UUID.randomUUID(), "Empresa Demo", "900123456-1",
                "empresa_demo", UUID.randomUUID(), "admin@demo.com", "temporal123");
        when(plataformaService.provisionarEmpresa(any(CuerpoNuevaEmpresa.class))).thenReturn(respuesta);

        mockMvc.perform(post("/api/platform/empresas")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.schemaNombre").value("empresa_demo"));
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void provisionarEmpresa_comoAdmin_devuelveForbidden() throws Exception {
        var cuerpo = new CuerpoNuevaEmpresa("Empresa Demo", "900123456-1", "empresa_demo",
                "admin@demo.com", "Admin Demo", "admin.demo", null);

        mockMvc.perform(post("/api/platform/empresas")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isForbidden());

        verify(plataformaService, never()).provisionarEmpresa(any());
    }

    @Test
    @WithMockUser(authorities = "ROLE_SUPERADMIN")
    void provisionarEmpresa_schemaConMayusculas_devuelveBadRequest() throws Exception {
        var cuerpo = new CuerpoNuevaEmpresa("Empresa Demo", "900123456-1", "EmpresaDemo",
                "admin@demo.com", "Admin Demo", "admin.demo", null);

        mockMvc.perform(post("/api/platform/empresas")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "ROLE_SUPERADMIN")
    void provisionarEmpresa_nitDuplicado_devuelveConflict() throws Exception {
        var cuerpo = new CuerpoNuevaEmpresa("Empresa Demo", "900123456-1", "empresa_demo",
                "admin@demo.com", "Admin Demo", "admin.demo", null);
        when(plataformaService.provisionarEmpresa(any(CuerpoNuevaEmpresa.class)))
                .thenThrow(new EmpresaYaExisteException("nit", "900123456-1"));

        mockMvc.perform(post("/api/platform/empresas")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(authorities = "ROLE_SUPERADMIN")
    void provisionarEmpresa_schemaInvalidoSegunService_devuelveBadRequest() throws Exception {
        var cuerpo = new CuerpoNuevaEmpresa("Empresa Demo", "900123456-1", "a", "admin@demo.com",
                "Admin Demo", "admin.demo", null);
        when(plataformaService.provisionarEmpresa(any(CuerpoNuevaEmpresa.class)))
                .thenThrow(new SchemaInvalidoException("a"));

        mockMvc.perform(post("/api/platform/empresas")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isBadRequest());
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
