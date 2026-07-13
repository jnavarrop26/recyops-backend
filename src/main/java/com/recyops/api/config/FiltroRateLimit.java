package com.recyops.api.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Rate limiting por IP con ventana fija de 1 minuto, en dos capas:
 *   - /api/auth/**  : limite estricto (anti fuerza bruta del login).
 *   - /api/**       : limite global mas holgado (contencion basica de abuso/DoS).
 *
 * La IP la resuelve {@link ResolutorIpCliente}, que solo confia en
 * X-Forwarded-For cuando hay un proxy de confianza configurado; asi el limite
 * no se puede evadir falsificando ese header en acceso directo.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class FiltroRateLimit extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(FiltroRateLimit.class);
    private static final int LIMPIAR_CADA_ENTRADAS = 20_000;

    private final ResolutorIpCliente resolutorIp;
    private final int maxAuthPorMinuto;
    private final int maxGlobalPorMinuto;
    private final Map<String, AtomicInteger> contadores = new ConcurrentHashMap<>();

    public FiltroRateLimit(ResolutorIpCliente resolutorIp,
            @Value("${recyops.auth.max-intentos-por-minuto}") int maxAuthPorMinuto,
            @Value("${recyops.rate-limit.global-por-minuto}") int maxGlobalPorMinuto) {
        this.resolutorIp = resolutorIp;
        this.maxAuthPorMinuto = maxAuthPorMinuto;
        this.maxGlobalPorMinuto = maxGlobalPorMinuto;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest peticion) {
        // Solo /api/**; el actuator y otros recursos quedan fuera
        return !peticion.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest peticion, HttpServletResponse respuesta,
            FilterChain cadena) throws ServletException, IOException {
        String ip = resolutorIp.resolver(peticion);
        boolean esAuth = peticion.getRequestURI().startsWith("/api/auth/");
        long ventana = System.currentTimeMillis() / 60_000;

        // Limpieza simple cuando el mapa acumula ventanas viejas
        if (contadores.size() > LIMPIAR_CADA_ENTRADAS) {
            contadores.clear();
        }

        // Capa global (aplica a todas las peticiones /api/**)
        if (superaLimite(ip + "|g|" + ventana, maxGlobalPorMinuto)) {
            rechazar(peticion, respuesta, ip, "global");
            return;
        }
        // Capa estricta del login
        if (esAuth && superaLimite(ip + "|a|" + ventana, maxAuthPorMinuto)) {
            rechazar(peticion, respuesta, ip, "auth");
            return;
        }
        cadena.doFilter(peticion, respuesta);
    }

    private boolean superaLimite(String llave, int maximo) {
        return contadores.computeIfAbsent(llave, k -> new AtomicInteger()).incrementAndGet() > maximo;
    }

    private void rechazar(HttpServletRequest peticion, HttpServletResponse respuesta, String ip,
            String capa) throws IOException {
        log.warn("Rate limit ({}) superado en {} desde {}", capa, peticion.getRequestURI(), ip);
        respuesta.setStatus(429);
        respuesta.setContentType("application/json;charset=UTF-8");
        respuesta.getWriter().write("""
                {"timestamp":"%s","status":429,"error":"Too Many Requests",\
                "mensaje":"Demasiadas peticiones; espera un minuto y vuelve a intentarlo",\
                "ruta":"%s"}""".formatted(LocalDateTime.now(), peticion.getRequestURI()));
    }
}
