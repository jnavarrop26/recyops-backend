package com.recyops.api.usuario.service;

import com.recyops.api.config.PropiedadesSupabase;
import com.recyops.api.usuario.excepciones.ErrorSupabaseAuthException;
import com.recyops.api.usuario.excepciones.UsuarioDuplicadoException;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Cliente de la Admin API de Supabase Auth (GoTrue). Usa la service_role key,
 * que solo vive en el backend: crea y elimina credenciales de usuarios.
 */
@Component
public class ClienteSupabaseAdmin {

    private static final Logger log = LoggerFactory.getLogger(ClienteSupabaseAdmin.class);

    private final RestClient cliente;

    public ClienteSupabaseAdmin(PropiedadesSupabase propiedades) {
        this.cliente = RestClient.builder()
                .baseUrl(propiedades.url())
                .defaultHeader("apikey", propiedades.serviceRoleKey())
                .defaultHeader("Authorization", "Bearer " + propiedades.serviceRoleKey())
                .build();
    }

    /**
     * Crea el usuario en Supabase Auth con los claims que el resto del sistema
     * espera en el JWT (rol y empresa_schema) y devuelve su id.
     */
    @SuppressWarnings("unchecked")
    public UUID crearUsuario(String email, String password, String nombreCompleto, String username,
            String rol, String esquemaEmpresa) {
        try {
            Map<String, Object> respuesta = cliente.post()
                    .uri("/auth/v1/admin/users")
                    .body(Map.of(
                            "email", email,
                            "password", password,
                            "email_confirm", true,
                            "user_metadata", Map.of(
                                    "nombre_completo", nombreCompleto,
                                    "username", username),
                            "app_metadata", Map.of(
                                    "rol", rol,
                                    "empresa_schema", esquemaEmpresa)))
                    .retrieve()
                    .body(Map.class);
            if (respuesta == null || respuesta.get("id") == null) {
                throw new ErrorSupabaseAuthException();
            }
            return UUID.fromString(String.valueOf(respuesta.get("id")));
        } catch (RestClientResponseException ex) {
            // GoTrue responde 422 (o 400/409 segun version) cuando el correo ya existe
            int estado = ex.getStatusCode().value();
            if (estado == 409 || estado == 422
                    || (estado == 400 && ex.getResponseBodyAsString().contains("already"))) {
                throw new UsuarioDuplicadoException("email (ya registrado en Supabase Auth)", email);
            }
            log.error("Supabase Auth respondio {} al crear usuario {}: {}",
                    estado, email, ex.getResponseBodyAsString());
            throw new ErrorSupabaseAuthException();
        }
    }

    /** Sincroniza nombre y rol en Supabase para que los proximos JWT traigan los claims correctos. */
    public void actualizarUsuario(UUID supabaseId, String nombreCompleto, String rol) {
        enviarActualizacion(supabaseId, Map.of(
                "user_metadata", Map.of("nombre_completo", nombreCompleto),
                "app_metadata", Map.of("rol", rol)));
    }

    /**
     * Bloquea o desbloquea el login del usuario en Supabase (ban). Desactivar un
     * trabajador sin esto seria decorativo: podria seguir iniciando sesion.
     */
    public void cambiarBloqueo(UUID supabaseId, boolean bloquear) {
        enviarActualizacion(supabaseId, Map.of("ban_duration", bloquear ? "876000h" : "none"));
    }

    private void enviarActualizacion(UUID supabaseId, Map<String, Object> cambios) {
        try {
            cliente.put()
                    .uri("/auth/v1/admin/users/{id}", supabaseId)
                    .body(cambios)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            log.error("Supabase Auth respondio {} al actualizar usuario {}: {}",
                    ex.getStatusCode().value(), supabaseId, ex.getResponseBodyAsString());
            throw new ErrorSupabaseAuthException();
        }
    }

    /** Elimina la credencial; se usa como rollback si el guardado local falla. */
    public void eliminarUsuario(UUID supabaseId) {
        try {
            cliente.delete()
                    .uri("/auth/v1/admin/users/{id}", supabaseId)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            // No se propaga: el rollback es best-effort y el detalle queda en el log
            log.error("No se pudo revertir el usuario {} en Supabase Auth: {}",
                    supabaseId, ex.getResponseBodyAsString());
        }
    }
}
