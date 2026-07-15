package com.recyops.api.tenant;

import com.recyops.api.comun.UsuarioAutenticado;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Lee el claim app_metadata.empresa_schema del JWT de Supabase y lo deja en
 * {@link ContextoEmpresa} para que Hibernate enrute la peticion al esquema
 * de la empresa. Tambien publica usuario y empresa en el MDC para que cada
 * linea de log quede contextualizada. Se registra dentro de la cadena de
 * seguridad (no como bean) para ejecutarse despues de la autenticacion.
 *
 * En modo estricto (produccion) un JWT autenticado SIN claim de empresa se
 * rechaza con 401: nadie cae "por accidente" al esquema por defecto.
 */
public class FiltroEsquemaEmpresa extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(FiltroEsquemaEmpresa.class);

    private final boolean esquemaEstricto;

    public FiltroEsquemaEmpresa(boolean esquemaEstricto) {
        this.esquemaEstricto = esquemaEstricto;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest peticion, HttpServletResponse respuesta,
            FilterChain cadena) throws ServletException, IOException {
        try {
            Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
            if (autenticacion instanceof JwtAuthenticationToken tokenJwt) {
                Map<String, Object> metadata = tokenJwt.getToken().getClaim("app_metadata");
                Object esquema = metadata != null ? metadata.get("empresa_schema") : null;
                if (esquema instanceof String nombre && ContextoEmpresa.esNombreValido(nombre)) {
                    ContextoEmpresa.establecer(nombre);
                    MDC.put("empresa", nombre);
                } else if (esSuperAdmin(autenticacion)) {
                    // Los super admins operan a nivel de plataforma, sin esquema de empresa
                    MDC.put("empresa", "PLATFORM");
                } else if (esquemaEstricto) {
                    log.warn("JWT sin claim empresa_schema rechazado (modo estricto): sub={}",
                            tokenJwt.getToken().getSubject());
                    responderSinEmpresa(peticion, respuesta);
                    return;
                }
                MDC.put("usuario", UsuarioAutenticado.nombreCompleto());
            }
            cadena.doFilter(peticion, respuesta);
        } finally {
            ContextoEmpresa.limpiar();
            MDC.remove("usuario");
            MDC.remove("empresa");
        }
    }

    private void responderSinEmpresa(HttpServletRequest peticion, HttpServletResponse respuesta)
            throws IOException {
        respuesta.setStatus(401);
        respuesta.setContentType("application/json;charset=UTF-8");
        respuesta.getWriter().write("""
                {"timestamp":"%s","status":401,"error":"Unauthorized",\
                "mensaje":"Tu usuario no tiene empresa asignada; contacta al administrador",\
                "ruta":"%s"}""".formatted(LocalDateTime.now(), peticion.getRequestURI()));
    }

    private boolean esSuperAdmin(Authentication autenticacion) {
        return autenticacion.getAuthorities().stream()
                .anyMatch(a -> "ROLE_SUPERADMIN".equals(a.getAuthority()));
    }
}
