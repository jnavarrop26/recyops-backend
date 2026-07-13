package com.recyops.api.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Determina la IP del cliente para el rate limiting.
 *
 * X-Forwarded-For lo puede falsificar cualquiera que pegue directo al servidor,
 * asi que solo se confia cuando 'confiar-en-proxy' esta activo (produccion detras
 * de Render/Vercel, donde el proxy fija ese header). En acceso directo se usa la
 * IP real del socket, que el cliente no puede suplantar.
 */
@Component
public class ResolutorIpCliente {

    private final boolean confiarEnProxy;

    public ResolutorIpCliente(@Value("${recyops.rate-limit.confiar-en-proxy}") boolean confiarEnProxy) {
        this.confiarEnProxy = confiarEnProxy;
    }

    public String resolver(HttpServletRequest peticion) {
        if (confiarEnProxy) {
            String reenviada = peticion.getHeader("X-Forwarded-For");
            if (reenviada != null && !reenviada.isBlank()) {
                // El primer valor es el cliente original; el resto son proxies
                return reenviada.split(",")[0].trim();
            }
        }
        return peticion.getRemoteAddr();
    }
}
