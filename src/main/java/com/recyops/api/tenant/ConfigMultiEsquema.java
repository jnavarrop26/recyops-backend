package com.recyops.api.tenant;

import org.hibernate.cfg.AvailableSettings;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Conecta el proveedor de conexiones y el resolutor de esquema con Hibernate.
 */
@Configuration
public class ConfigMultiEsquema {

    @Bean
    HibernatePropertiesCustomizer personalizadorMultiEsquema(
            ProveedorConexionesPorEsquema proveedorConexiones,
            ResolutorEsquemaEmpresa resolutorEsquema) {
        return propiedades -> {
            propiedades.put(AvailableSettings.MULTI_TENANT_CONNECTION_PROVIDER, proveedorConexiones);
            propiedades.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, resolutorEsquema);
        };
    }
}
