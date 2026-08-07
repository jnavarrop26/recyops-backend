package com.recyops.api.unit.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.recyops.api.auth.dtos.CuerpoLogin;
import com.recyops.api.auth.dtos.CuerpoRecuperar;
import com.recyops.api.auth.dtos.CuerpoRefresh;
import com.recyops.api.auth.dtos.CuerpoRestablecer;
import com.recyops.api.auth.excepciones.CredencialesInvalidasException;
import com.recyops.api.auth.excepciones.SesionExpiradaException;
import com.recyops.api.auth.service.AuthServiceImpl;
import com.recyops.api.comun.excepciones.ReglaNegocioException;
import com.recyops.api.config.PropiedadesSupabase;
import java.net.URI;
import java.util.Map;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;

/**
 * AuthServiceImpl delega en Supabase Auth via RestClient. El RestClient real
 * (construido en el constructor a partir del RestClient.Builder inyectado) se
 * mockea de punta a punta: no se ejercita ninguna llamada de red real.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private RestClient.Builder restClientBuilder;

    @Mock
    private RestClient clienteSupabase;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        when(restClientBuilder.baseUrl(anyString())).thenReturn(restClientBuilder);
        when(restClientBuilder.defaultHeader(anyString(), any())).thenReturn(restClientBuilder);
        when(restClientBuilder.build()).thenReturn(clienteSupabase);

        var propiedades = new PropiedadesSupabase("https://demo.supabase.co", "anon-key", "service-key",
                "empresa_demo");
        authService = new AuthServiceImpl(restClientBuilder, propiedades,
                "http://localhost:5173,http://otro-origen.com");
    }

    // ---- login ----

    @Test
    void login_credencialesValidas_retornaRespuestaLogin() {
        // Given
        var cuerpo = new CuerpoLogin("ana@recyops.com", "clave123");
        var bodyUriSpec = mockPostToken();
        var captorCuerpo = ArgumentCaptor.forClass(Object.class);
        when(bodyUriSpec.body(captorCuerpo.capture())).thenReturn(bodyUriSpec);
        var responseSpec = mock(RestClient.ResponseSpec.class);
        when(bodyUriSpec.retrieve()).thenReturn(responseSpec);
        Map<String, Object> usuario = Map.of(
                "email", "ana@recyops.com",
                "user_metadata", Map.of("nombre_completo", "Ana Perez", "username", "ana"),
                "app_metadata", Map.of("rol", "ADMIN"));
        Map<String, Object> mapaSupabase = Map.of(
                "access_token", "token-abc",
                "refresh_token", "refresh-abc",
                "user", usuario);
        when(responseSpec.body(Map.class)).thenReturn(mapaSupabase);

        // When
        var actualRespuesta = authService.login(cuerpo);

        // Then
        assertThat(actualRespuesta.token()).isEqualTo("token-abc");
        assertThat(actualRespuesta.refreshToken()).isEqualTo("refresh-abc");
        assertThat(actualRespuesta.rol()).isEqualTo("ADMIN");
        assertThat(actualRespuesta.nombreCompleto()).isEqualTo("Ana Perez");
        assertThat(actualRespuesta.username()).isEqualTo("ana");
        @SuppressWarnings("unchecked")
        var cuerpoEnviado = (Map<String, String>) captorCuerpo.getValue();
        assertThat(cuerpoEnviado).containsEntry("email", "ana@recyops.com").containsEntry("password", "clave123");
    }

    @Test
    void login_credencialesInvalidas_lanzaCredencialesInvalidasException() {
        // Given
        var cuerpo = new CuerpoLogin("ana@recyops.com", "clave-incorrecta");
        var bodyUriSpec = mockPostToken();
        when(bodyUriSpec.body(any(Object.class))).thenReturn(bodyUriSpec);
        var excepcionSupabase = HttpClientErrorException.create(
                HttpStatus.BAD_REQUEST, "Bad Request", HttpHeaders.EMPTY, new byte[0], null);
        when(bodyUriSpec.retrieve()).thenThrow(excepcionSupabase);

        // When-Then
        assertThatThrownBy(() -> authService.login(cuerpo))
                .isInstanceOf(CredencialesInvalidasException.class);
    }

    @Test
    void login_respuestaSinAccessToken_lanzaCredencialesInvalidasException() {
        // Given
        var cuerpo = new CuerpoLogin("ana@recyops.com", "clave123");
        var bodyUriSpec = mockPostToken();
        when(bodyUriSpec.body(any(Object.class))).thenReturn(bodyUriSpec);
        var responseSpec = mock(RestClient.ResponseSpec.class);
        when(bodyUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(Map.class)).thenReturn(Map.of());

        // When-Then
        assertThatThrownBy(() -> authService.login(cuerpo))
                .isInstanceOf(CredencialesInvalidasException.class);
    }

    // ---- refrescar ----

    @Test
    void refrescar_tokenValido_retornaRespuestaLoginConRolPorDefecto() {
        // Given
        var cuerpo = new CuerpoRefresh("refresh-valido");
        var bodyUriSpec = mockPostToken();
        var captorCuerpo = ArgumentCaptor.forClass(Object.class);
        when(bodyUriSpec.body(captorCuerpo.capture())).thenReturn(bodyUriSpec);
        var responseSpec = mock(RestClient.ResponseSpec.class);
        when(bodyUriSpec.retrieve()).thenReturn(responseSpec);
        Map<String, Object> usuario = Map.of("email", "ana@recyops.com");
        Map<String, Object> mapaSupabase = Map.of(
                "access_token", "token-nuevo",
                "refresh_token", "refresh-nuevo",
                "user", usuario);
        when(responseSpec.body(Map.class)).thenReturn(mapaSupabase);

        // When
        var actualRespuesta = authService.refrescar(cuerpo);

        // Then: sin app_metadata.rol ni user_metadata, cae a los valores por defecto
        assertThat(actualRespuesta.token()).isEqualTo("token-nuevo");
        assertThat(actualRespuesta.refreshToken()).isEqualTo("refresh-nuevo");
        assertThat(actualRespuesta.rol()).isEqualTo("OPERARIO");
        assertThat(actualRespuesta.nombreCompleto()).isEqualTo("ana@recyops.com");
        @SuppressWarnings("unchecked")
        var cuerpoEnviado = (Map<String, String>) captorCuerpo.getValue();
        assertThat(cuerpoEnviado).containsEntry("refresh_token", "refresh-valido");
    }

    @Test
    void refrescar_tokenInvalido_lanzaSesionExpiradaException() {
        // Given
        var cuerpo = new CuerpoRefresh("refresh-invalido");
        var bodyUriSpec = mockPostToken();
        when(bodyUriSpec.body(any(Object.class))).thenReturn(bodyUriSpec);
        var excepcionSupabase = HttpClientErrorException.create(
                HttpStatus.UNAUTHORIZED, "Unauthorized", HttpHeaders.EMPTY, new byte[0], null);
        when(bodyUriSpec.retrieve()).thenThrow(excepcionSupabase);

        // When-Then
        assertThatThrownBy(() -> authService.refrescar(cuerpo))
                .isInstanceOf(SesionExpiradaException.class);
    }

    // ---- recuperarPassword ----

    @Test
    void recuperarPassword_correoValido_enviaSolicitudDeRecuperacion() {
        // Given
        var cuerpo = new CuerpoRecuperar("ana@recyops.com");
        var bodyUriSpec = mockPostRecuperar();
        var captorCuerpo = ArgumentCaptor.forClass(Object.class);
        when(bodyUriSpec.body(captorCuerpo.capture())).thenReturn(bodyUriSpec);
        var responseSpec = mock(RestClient.ResponseSpec.class);
        when(bodyUriSpec.retrieve()).thenReturn(responseSpec);

        // When
        authService.recuperarPassword(cuerpo);

        // Then
        verify(responseSpec).toBodilessEntity();
        @SuppressWarnings("unchecked")
        var cuerpoEnviado = (Map<String, String>) captorCuerpo.getValue();
        assertThat(cuerpoEnviado).containsEntry("email", "ana@recyops.com");
    }

    @Test
    void recuperarPassword_supabaseRespondeDemasiadasSolicitudes_lanzaReglaNegocioException() {
        // Given
        var cuerpo = new CuerpoRecuperar("ana@recyops.com");
        var bodyUriSpec = mockPostRecuperar();
        when(bodyUriSpec.body(any(Object.class))).thenReturn(bodyUriSpec);
        var excepcionSupabase = HttpClientErrorException.create(
                HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", HttpHeaders.EMPTY, new byte[0], null);
        when(bodyUriSpec.retrieve()).thenThrow(excepcionSupabase);

        // When-Then
        assertThatThrownBy(() -> authService.recuperarPassword(cuerpo))
                .isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void recuperarPassword_correoNoRegistrado_noLanzaExcepcion() {
        // Given: Supabase responde error (p.ej. correo inexistente), pero el metodo
        // nunca revela si el correo existe: solo queda registrado en el log.
        var cuerpo = new CuerpoRecuperar("desconocido@recyops.com");
        var bodyUriSpec = mockPostRecuperar();
        when(bodyUriSpec.body(any(Object.class))).thenReturn(bodyUriSpec);
        var excepcionSupabase = HttpClientErrorException.create(
                HttpStatus.NOT_FOUND, "Not Found", HttpHeaders.EMPTY, new byte[0], null);
        when(bodyUriSpec.retrieve()).thenThrow(excepcionSupabase);

        // When-Then
        assertThatCode(() -> authService.recuperarPassword(cuerpo)).doesNotThrowAnyException();
    }

    // ---- restablecerPassword ----

    @Test
    void restablecerPassword_tokenValido_actualizaContrasena() {
        // Given
        var cuerpo = new CuerpoRestablecer("access-token-valido", "nuevaClave123");
        var bodyUriSpec = mockPutRestablecer();
        var captorCuerpo = ArgumentCaptor.forClass(Object.class);
        when(bodyUriSpec.body(captorCuerpo.capture())).thenReturn(bodyUriSpec);
        var responseSpec = mock(RestClient.ResponseSpec.class);
        when(bodyUriSpec.retrieve()).thenReturn(responseSpec);

        // When
        authService.restablecerPassword(cuerpo);

        // Then
        verify(responseSpec).toBodilessEntity();
        @SuppressWarnings("unchecked")
        var cuerpoEnviado = (Map<String, String>) captorCuerpo.getValue();
        assertThat(cuerpoEnviado).containsEntry("password", "nuevaClave123");
    }

    @Test
    void restablecerPassword_tokenExpirado_lanzaSesionExpiradaException() {
        // Given
        var cuerpo = new CuerpoRestablecer("access-token-expirado", "nuevaClave123");
        var bodyUriSpec = mockPutRestablecer();
        when(bodyUriSpec.body(any(Object.class))).thenReturn(bodyUriSpec);
        var excepcionSupabase = HttpClientErrorException.create(
                HttpStatus.UNAUTHORIZED, "Unauthorized", HttpHeaders.EMPTY, new byte[0], null);
        when(bodyUriSpec.retrieve()).thenThrow(excepcionSupabase);

        // When-Then
        assertThatThrownBy(() -> authService.restablecerPassword(cuerpo))
                .isInstanceOf(SesionExpiradaException.class);
    }

    @Test
    void restablecerPassword_contrasenaNoCumpleRequisitos_lanzaReglaNegocioException() {
        // Given
        var cuerpo = new CuerpoRestablecer("access-token-valido", "123456");
        var bodyUriSpec = mockPutRestablecer();
        when(bodyUriSpec.body(any(Object.class))).thenReturn(bodyUriSpec);
        var excepcionSupabase = HttpClientErrorException.create(
                HttpStatus.UNPROCESSABLE_ENTITY, "Unprocessable Entity", HttpHeaders.EMPTY, new byte[0], null);
        when(bodyUriSpec.retrieve()).thenThrow(excepcionSupabase);

        // When-Then
        assertThatThrownBy(() -> authService.restablecerPassword(cuerpo))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La nueva contrasena no cumple los requisitos de seguridad");
    }

    @Test
    void restablecerPassword_errorInesperadoDeSupabase_lanzaReglaNegocioException() {
        // Given
        var cuerpo = new CuerpoRestablecer("access-token-valido", "nuevaClave123");
        var bodyUriSpec = mockPutRestablecer();
        when(bodyUriSpec.body(any(Object.class))).thenReturn(bodyUriSpec);
        var excepcionSupabase = HttpServerErrorException.create(
                HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", HttpHeaders.EMPTY, new byte[0], null);
        when(bodyUriSpec.retrieve()).thenThrow(excepcionSupabase);

        // When-Then
        assertThatThrownBy(() -> authService.restablecerPassword(cuerpo))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se pudo restablecer la contrasena; intenta de nuevo");
    }

    // ---- helpers de mocking del RestClient fluido ----

    private RestClient.RequestBodyUriSpec mockPostToken() {
        RestClient.RequestBodyUriSpec bodyUriSpec = mock(RestClient.RequestBodyUriSpec.class);
        when(clienteSupabase.post()).thenReturn(bodyUriSpec);
        when(bodyUriSpec.uri(anyString())).thenReturn(bodyUriSpec);
        return bodyUriSpec;
    }

    private RestClient.RequestBodyUriSpec mockPostRecuperar() {
        RestClient.RequestBodyUriSpec bodyUriSpec = mock(RestClient.RequestBodyUriSpec.class);
        when(clienteSupabase.post()).thenReturn(bodyUriSpec);
        when(bodyUriSpec.uri(ArgumentMatchers.<Function<UriBuilder, URI>>any())).thenReturn(bodyUriSpec);
        return bodyUriSpec;
    }

    private RestClient.RequestBodyUriSpec mockPutRestablecer() {
        RestClient.RequestBodyUriSpec bodyUriSpec = mock(RestClient.RequestBodyUriSpec.class);
        when(clienteSupabase.put()).thenReturn(bodyUriSpec);
        when(bodyUriSpec.uri(anyString())).thenReturn(bodyUriSpec);
        when(bodyUriSpec.header(anyString(), anyString())).thenReturn(bodyUriSpec);
        return bodyUriSpec;
    }
}
