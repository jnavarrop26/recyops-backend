package com.recyops.api.tenant;

import com.recyops.api.config.PropiedadesSupabase;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.stereotype.Component;

/**
 * Indica a Hibernate cual esquema usar en la sesion actual:
 * el de la empresa autenticada o, en su defecto, el esquema por defecto.
 */
@Component
public class ResolutorEsquemaEmpresa implements CurrentTenantIdentifierResolver<String> {

    private final String esquemaPorDefecto;

    public ResolutorEsquemaEmpresa(PropiedadesSupabase propiedades) {
        this.esquemaPorDefecto = propiedades.esquemaPorDefecto();
    }

    @Override
    public String resolveCurrentTenantIdentifier() {
        String esquema = ContextoEmpresa.obtener();
        return esquema != null ? esquema : esquemaPorDefecto;
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return false;
    }
}
