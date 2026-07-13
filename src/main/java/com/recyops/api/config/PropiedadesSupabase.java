package com.recyops.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propiedades de conexion con Supabase (Auth y datos).
 */
@ConfigurationProperties(prefix = "supabase")
public record PropiedadesSupabase(
        String url,
        String anonKey,
        String serviceRoleKey,
        String esquemaPorDefecto) {
}
