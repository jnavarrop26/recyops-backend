package com.recyops.api.tenant;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.springframework.stereotype.Component;

/**
 * Entrega conexiones del pool con el search_path apuntando al esquema
 * de la empresa actual. Asi cada empresa lee y escribe solo sus tablas
 * sin compartir filas con otras (aislamiento por esquema, no por fila).
 */
@Component
public class ProveedorConexionesPorEsquema implements MultiTenantConnectionProvider<String> {

    private final transient DataSource dataSource;

    public ProveedorConexionesPorEsquema(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Connection getAnyConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void releaseAnyConnection(Connection conexion) throws SQLException {
        conexion.close();
    }

    @Override
    public Connection getConnection(String esquema) throws SQLException {
        if (!ContextoEmpresa.esNombreValido(esquema)) {
            throw new SQLException("Esquema de empresa invalido: " + esquema);
        }
        Connection conexion = dataSource.getConnection();
        try (Statement sentencia = conexion.createStatement()) {
            sentencia.execute("SET search_path TO " + esquema);
        }
        return conexion;
    }

    @Override
    public void releaseConnection(String esquema, Connection conexion) throws SQLException {
        try (Statement sentencia = conexion.createStatement()) {
            sentencia.execute("SET search_path TO public");
        } finally {
            conexion.close();
        }
    }

    @Override
    public boolean supportsAggressiveRelease() {
        return false;
    }

    @Override
    public boolean isUnwrappableAs(Class<?> tipo) {
        return tipo.isInstance(this);
    }

    @Override
    public <T> T unwrap(Class<T> tipo) {
        if (isUnwrappableAs(tipo)) {
            return tipo.cast(this);
        }
        throw new IllegalArgumentException("No se puede convertir a " + tipo);
    }
}
