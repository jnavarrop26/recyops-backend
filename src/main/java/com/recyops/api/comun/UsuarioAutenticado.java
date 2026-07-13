package com.recyops.api.comun;

import java.util.Map;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Acceso rapido a los datos del usuario autenticado (claims del JWT de Supabase).
 */
public final class UsuarioAutenticado {

    private UsuarioAutenticado() {
    }

    public static String nombreCompleto() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion instanceof JwtAuthenticationToken tokenJwt) {
            Map<String, Object> metadata = tokenJwt.getToken().getClaim("user_metadata");
            if (metadata != null && metadata.get("nombre_completo") instanceof String nombre && !nombre.isBlank()) {
                return nombre;
            }
            return tokenJwt.getToken().getSubject();
        }
        return "sistema";
    }

    /** Id del usuario en Supabase Auth (claim sub); enlaza con usuarios.supabase_id. */
    public static UUID supabaseId() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion instanceof JwtAuthenticationToken tokenJwt) {
            try {
                return UUID.fromString(tokenJwt.getToken().getSubject());
            } catch (IllegalArgumentException ignorada) {
                return null;
            }
        }
        return null;
    }

    public static boolean esAdmin() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        return autenticacion != null && autenticacion.getAuthorities().stream()
                .anyMatch(autoridad -> "ROLE_ADMIN".equals(autoridad.getAuthority()));
    }
}
