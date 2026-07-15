package com.recyops.api.config;

import com.recyops.api.tenant.FiltroEsquemaEmpresa;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Seguridad del monolito: API stateless que valida JWT emitidos por Supabase Auth.
 * El rol del usuario viaja en el claim app_metadata.rol (ADMIN / OPERARIO).
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${recyops.cors.origenes-permitidos}")
    private String origenesPermitidos;

    @Value("${recyops.esquema-estricto}")
    private boolean esquemaEstricto;

    @Bean
    SecurityFilterChain filtroSeguridad(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/actuator/prometheus").hasRole("ADMIN")
                        .requestMatchers("/api/platform/**").hasRole("SUPERADMIN")
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // El operario LEE los catalogos que necesita para operar
                        // (bodegas y materiales alimentan el formulario de ingreso)
                        .requestMatchers(HttpMethod.GET, "/api/bodegas").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/materiales", "/api/materiales/categorias",
                                "/api/materiales/subcategorias", "/api/materiales/resinas",
                                "/api/materiales/colores").authenticated()

                        // Todo lo demas de catalogos y modulos administrativos: solo ADMIN.
                        // La UI ya lo esconde (RutaAdmin), pero la regla real vive aqui.
                        .requestMatchers("/api/bodegas/**").hasRole("ADMIN")
                        .requestMatchers("/api/materiales/**").hasRole("ADMIN")
                        .requestMatchers("/api/proveedores/**").hasRole("ADMIN")
                        .requestMatchers("/api/convenios/**").hasRole("ADMIN")
                        .requestMatchers("/api/entregas/**").hasRole("ADMIN")
                        .requestMatchers("/api/inventario/**").hasRole("ADMIN")
                        .requestMatchers("/api/roles/**").hasRole("ADMIN")

                        // Lo que queda (ingresos, tareas, dashboard) exige sesion;
                        // tareas ademas valida rol/propiedad en su service.
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(convertidorRolesSupabase())))
                // Selecciona el esquema de la empresa despues de autenticar el JWT
                .addFilterAfter(new FiltroEsquemaEmpresa(esquemaEstricto), AuthorizationFilter.class);
        return http.build();
    }

    /**
     * Convierte app_metadata.rol del JWT de Supabase en una autoridad ROLE_*.
     */
    private JwtAuthenticationConverter convertidorRolesSupabase() {
        JwtAuthenticationConverter convertidor = new JwtAuthenticationConverter();
        convertidor.setJwtGrantedAuthoritiesConverter(jwt -> {
            Map<String, Object> metadata = jwt.getClaim("app_metadata");
            Object rol = metadata != null ? metadata.get("rol") : null;
            if (rol instanceof String nombreRol && !nombreRol.isBlank()) {
                return List.<GrantedAuthority>of(new SimpleGrantedAuthority("ROLE_" + nombreRol.toUpperCase()));
            }
            return List.of();
        });
        return convertidor;
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList(origenesPermitidos.split(",")));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/**", config);
        return fuente;
    }
}
