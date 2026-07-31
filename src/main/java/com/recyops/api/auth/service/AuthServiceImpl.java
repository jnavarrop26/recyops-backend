package com.recyops.api.auth.service;

import com.recyops.api.auth.dtos.CuerpoLogin;
import com.recyops.api.auth.dtos.CuerpoRecuperar;
import com.recyops.api.auth.dtos.CuerpoRefresh;
import com.recyops.api.auth.dtos.CuerpoRestablecer;
import com.recyops.api.auth.dtos.RespuestaLogin;
import com.recyops.api.auth.excepciones.CredencialesInvalidasException;
import com.recyops.api.auth.excepciones.SesionExpiradaException;
import com.recyops.api.auth.interfaces.AuthService;
import com.recyops.api.comun.excepciones.ReglaNegocioException;
import com.recyops.api.config.PropiedadesSupabase;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Delega la autenticacion en Supabase Auth (GoTrue): login con password,
 * renovacion con refresh token (rotado en cada uso) y recuperacion de contrasena.
 */
@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final RestClient clienteSupabase;
    private final String urlRestablecer;

    public AuthServiceImpl(RestClient.Builder restClientBuilder, PropiedadesSupabase propiedades,
            @Value("${recyops.cors.origenes-permitidos}") String origenesPermitidos) {
        // El builder inyectado (no RestClient.builder() estatico) respeta
        // spring.http.client.connect-timeout/read-timeout: sin esto, un
        // Supabase Auth colgado agotaria hilos de Tomcat sin limite.
        this.clienteSupabase = restClientBuilder
                .baseUrl(propiedades.url())
                .defaultHeader("apikey", propiedades.anonKey())
                .build();
        // El enlace del correo de recuperacion vuelve a la pantalla de
        // restablecer del cliente (primer origen configurado en CORS).
        this.urlRestablecer = origenesPermitidos.split(",")[0].trim() + "/restablecer";
    }

    @Override
    public RespuestaLogin login(CuerpoLogin cuerpo) {
        Map<String, Object> respuesta;
        try {
            respuesta = pedirToken("password",
                    Map.of("email", cuerpo.username(), "password", cuerpo.password()));
        } catch (RestClientResponseException ex) {
            throw new CredencialesInvalidasException();
        }
        return mapear(respuesta, cuerpo.username());
    }

    @Override
    public RespuestaLogin refrescar(CuerpoRefresh cuerpo) {
        Map<String, Object> respuesta;
        try {
            respuesta = pedirToken("refresh_token",
                    Map.of("refresh_token", cuerpo.refreshToken()));
        } catch (RestClientResponseException ex) {
            throw new SesionExpiradaException();
        }
        return mapear(respuesta, null);
    }

    @Override
    public void recuperarPassword(CuerpoRecuperar cuerpo) {
        try {
            clienteSupabase.post()
                    .uri(builder -> builder.path("/auth/v1/recover")
                            .queryParam("redirect_to", urlRestablecer)
                            .build())
                    .body(Map.of("email", cuerpo.email()))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 429) {
                throw new ReglaNegocioException("Ya se envio un correo hace poco; espera un momento e intenta de nuevo");
            }
            // No se revela si el correo existe o no: se responde OK igual,
            // pero el detalle queda en los logs para diagnostico.
            log.warn("Supabase respondio {} al pedir recuperacion: {}",
                    ex.getStatusCode().value(), ex.getResponseBodyAsString());
        }
    }

    @Override
    public void restablecerPassword(CuerpoRestablecer cuerpo) {
        try {
            clienteSupabase.put()
                    .uri("/auth/v1/user")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + cuerpo.accessToken())
                    .body(Map.of("password", cuerpo.password()))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            int estado = ex.getStatusCode().value();
            if (estado == 401 || estado == 403) {
                throw new SesionExpiradaException();
            }
            if (estado == 422 || estado == 400) {
                throw new ReglaNegocioException("La nueva contrasena no cumple los requisitos de seguridad");
            }
            log.error("Supabase respondio {} al restablecer contrasena: {}",
                    estado, ex.getResponseBodyAsString());
            throw new ReglaNegocioException("No se pudo restablecer la contrasena; intenta de nuevo");
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> pedirToken(String tipoGrant, Map<String, String> cuerpo) {
        return clienteSupabase.post()
                .uri("/auth/v1/token?grant_type=" + tipoGrant)
                .body(cuerpo)
                .retrieve()
                .body(Map.class);
    }

    @SuppressWarnings("unchecked")
    private RespuestaLogin mapear(Map<String, Object> respuesta, String usernameFallback) {
        if (respuesta == null || respuesta.get("access_token") == null) {
            throw new CredencialesInvalidasException();
        }
        Map<String, Object> usuario = (Map<String, Object>) respuesta.getOrDefault("user", Map.of());
        Map<String, Object> userMetadata = (Map<String, Object>) usuario.getOrDefault("user_metadata", Map.of());
        Map<String, Object> appMetadata = (Map<String, Object>) usuario.getOrDefault("app_metadata", Map.of());
        String email = usernameFallback != null ? usernameFallback
                : String.valueOf(usuario.getOrDefault("email", ""));

        return new RespuestaLogin(
                (String) respuesta.get("access_token"),
                (String) respuesta.get("refresh_token"),
                String.valueOf(appMetadata.getOrDefault("rol", "OPERARIO")),
                String.valueOf(userMetadata.getOrDefault("nombre_completo", email)),
                String.valueOf(userMetadata.getOrDefault("username", email)));
    }
}
