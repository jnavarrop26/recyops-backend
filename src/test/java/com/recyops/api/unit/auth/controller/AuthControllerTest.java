package com.recyops.api.unit.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.recyops.api.auth.controller.AuthController;
import com.recyops.api.auth.dtos.CuerpoLogin;
import com.recyops.api.auth.dtos.CuerpoRecuperar;
import com.recyops.api.auth.dtos.CuerpoRefresh;
import com.recyops.api.auth.dtos.CuerpoRestablecer;
import com.recyops.api.auth.dtos.RespuestaLogin;
import com.recyops.api.auth.excepciones.CredencialesInvalidasException;
import com.recyops.api.auth.excepciones.SesionExpiradaException;
import com.recyops.api.auth.interfaces.AuthService;
import com.recyops.api.config.FiltroRateLimit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = AuthController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = FiltroRateLimit.class))
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    // ---------- login ----------

    @Test
    void login_credencialesValidas_devuelveOkConToken() throws Exception {
        var cuerpo = new CuerpoLogin("admin@test.com", "password123");
        when(authService.login(cuerpo)).thenReturn(
                new RespuestaLogin("token-jwt", "refresh-token", "ADMIN", "Admin Uno", "admin@test.com"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token-jwt"))
                .andExpect(jsonPath("$.rol").value("ADMIN"));
    }

    @Test
    void login_credencialesInvalidas_devuelveUnauthorized() throws Exception {
        var cuerpo = new CuerpoLogin("admin@test.com", "incorrecta");
        when(authService.login(cuerpo)).thenThrow(new CredencialesInvalidasException());

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void login_usernameEnBlanco_devuelveBadRequest() throws Exception {
        var cuerpo = new CuerpoLogin("", "password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isBadRequest());

        verify(authService, org.mockito.Mockito.never()).login(any());
    }

    // ---------- refresh ----------

    @Test
    void refrescar_tokenValido_devuelveOk() throws Exception {
        var cuerpo = new CuerpoRefresh("refresh-valido");
        when(authService.refrescar(cuerpo)).thenReturn(
                new RespuestaLogin("nuevo-token", "nuevo-refresh", "OPERARIO", "Juan Perez", "operario@test.com"));

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("nuevo-token"));
    }

    @Test
    void refrescar_tokenExpirado_devuelveUnauthorized() throws Exception {
        var cuerpo = new CuerpoRefresh("refresh-vencido");
        when(authService.refrescar(cuerpo)).thenThrow(new SesionExpiradaException());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refrescar_refreshTokenEnBlanco_devuelveBadRequest() throws Exception {
        var cuerpo = new CuerpoRefresh("");

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isBadRequest());
    }

    // ---------- recuperar ----------

    @Test
    void recuperar_emailValido_devuelveOk() throws Exception {
        var cuerpo = new CuerpoRecuperar("usuario@test.com");

        mockMvc.perform(post("/api/auth/recuperar")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isOk());

        verify(authService).recuperarPassword(cuerpo);
    }

    @Test
    void recuperar_emailInvalido_devuelveBadRequest() throws Exception {
        var cuerpo = new CuerpoRecuperar("no-es-un-email");

        mockMvc.perform(post("/api/auth/recuperar")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isBadRequest());
    }

    // ---------- restablecer ----------

    @Test
    void restablecer_cuerpoValido_devuelveOk() throws Exception {
        var cuerpo = new CuerpoRestablecer("access-token", "nuevaClave123");

        mockMvc.perform(post("/api/auth/restablecer")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isOk());

        verify(authService).restablecerPassword(cuerpo);
    }

    @Test
    void restablecer_passwordCorta_devuelveBadRequest() throws Exception {
        var cuerpo = new CuerpoRestablecer("access-token", "abc");

        mockMvc.perform(post("/api/auth/restablecer")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje", org.hamcrest.Matchers.containsString("al menos 6 caracteres")));
    }
}
