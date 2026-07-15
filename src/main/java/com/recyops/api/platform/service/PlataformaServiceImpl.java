package com.recyops.api.platform.service;

import com.recyops.api.comun.log.LogTransaccional;
import com.recyops.api.platform.dtos.CuerpoNuevaEmpresa;
import com.recyops.api.platform.dtos.RespuestaEmpresaCreada;
import com.recyops.api.platform.excepciones.EmpresaYaExisteException;
import com.recyops.api.platform.excepciones.SchemaInvalidoException;
import com.recyops.api.platform.interfaces.PlataformaService;
import com.recyops.api.tenant.ContextoEmpresa;
import com.recyops.api.usuario.service.ClienteSupabaseAdmin;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.Statement;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

@Service
public class PlataformaServiceImpl implements PlataformaService {

    private static final Logger log = LoggerFactory.getLogger(PlataformaServiceImpl.class);
    private static final String CHARS_PASSWORD =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%&*";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final DataSource dataSource;
    private final JdbcTemplate jdbc;
    private final ClienteSupabaseAdmin supabaseAdmin;

    public PlataformaServiceImpl(DataSource dataSource, JdbcTemplate jdbc,
            ClienteSupabaseAdmin supabaseAdmin) {
        this.dataSource = dataSource;
        this.jdbc = jdbc;
        this.supabaseAdmin = supabaseAdmin;
    }

    @Override
    @LogTransaccional(operacion = "EMPRESA_PROVISIONADA")
    public RespuestaEmpresaCreada provisionarEmpresa(CuerpoNuevaEmpresa cuerpo) {
        if (!ContextoEmpresa.esNombreValido(cuerpo.schemaNombre())) {
            throw new SchemaInvalidoException(cuerpo.schemaNombre());
        }
        verificarUnicidadNit(cuerpo.nit());
        verificarUnicidadSchema(cuerpo.schemaNombre());

        ejecutarDdlEsquema(cuerpo.schemaNombre());

        UUID supabaseId = null;
        try {
            boolean passwordPropia = cuerpo.adminPassword() != null && !cuerpo.adminPassword().isBlank();
            String passwordTemporal = passwordPropia ? null : generarPassword();
            String passwordFinal = passwordPropia ? cuerpo.adminPassword() : passwordTemporal;

            supabaseId = supabaseAdmin.crearUsuario(
                    cuerpo.adminEmail(),
                    passwordFinal,
                    cuerpo.adminNombreCompleto(),
                    cuerpo.adminUsername(),
                    "ADMIN",
                    cuerpo.schemaNombre());

            guardarPerfilAdmin(supabaseId, cuerpo.adminNombreCompleto(),
                    cuerpo.adminUsername(), cuerpo.adminEmail(), cuerpo.schemaNombre());

            UUID empresaId = UUID.randomUUID();
            jdbc.update("""
                    INSERT INTO public.empresas (id, nombre, nit, schema_name)
                    VALUES (?, ?, ?, ?)
                    """, empresaId, cuerpo.nombre(), cuerpo.nit(), cuerpo.schemaNombre());

            return new RespuestaEmpresaCreada(empresaId, cuerpo.nombre(), cuerpo.nit(),
                    cuerpo.schemaNombre(), supabaseId, cuerpo.adminEmail(), passwordTemporal);

        } catch (Exception ex) {
            if (supabaseId != null) {
                supabaseAdmin.eliminarUsuario(supabaseId);
            }
            eliminarEsquema(cuerpo.schemaNombre());
            throw ex;
        }
    }

    private void verificarUnicidadNit(String nit) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM public.empresas WHERE nit = ?", Integer.class, nit);
        if (count != null && count > 0) {
            throw new EmpresaYaExisteException("nit", nit);
        }
    }

    private void verificarUnicidadSchema(String schema) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM public.empresas WHERE schema_name = ?", Integer.class, schema);
        if (count != null && count > 0) {
            throw new EmpresaYaExisteException("schema_name", schema);
        }
    }

    private void ejecutarDdlEsquema(String esquema) {
        try {
            String sql = cargarDdl(esquema);
            try (Connection conexion = dataSource.getConnection()) {
                boolean autoCommitOriginal = conexion.getAutoCommit();
                conexion.setAutoCommit(false);
                try (Statement stmt = conexion.createStatement()) {
                    for (String sentencia : sql.split(";")) {
                        String limpio = sentencia.strip();
                        if (!limpio.isEmpty()) {
                            stmt.execute(limpio);
                        }
                    }
                    conexion.commit();
                } catch (Exception ex) {
                    conexion.rollback();
                    throw ex;
                } finally {
                    // Garantiza que la conexion vuelve al pool con el search_path correcto
                    try (Statement reset = conexion.createStatement()) {
                        reset.execute("SET search_path TO public");
                    }
                    conexion.setAutoCommit(autoCommitOriginal);
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException("Error al crear el esquema de la empresa: " + ex.getMessage(), ex);
        }
    }

    private String cargarDdl(String esquema) throws Exception {
        var resource = new ClassPathResource("db/esquema_empresa.sql");
        return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8)
                .replace("{{esquema}}", esquema);
    }

    private void guardarPerfilAdmin(UUID supabaseId, String nombreCompleto, String username,
            String email, String esquema) {
        // esquema ya fue validado por ContextoEmpresa.esNombreValido() (^[a-z][a-z0-9_]{0,62}$)
        String sql = """
                INSERT INTO %s.usuarios (supabase_id, nombre_completo, username, email, rol_id)
                SELECT ?, ?, ?, ?, id FROM %s.roles WHERE nombre = 'ADMIN'
                """.formatted(esquema, esquema);
        jdbc.update(sql, supabaseId, nombreCompleto, username, email);
    }

    private void eliminarEsquema(String esquema) {
        try (Connection conexion = dataSource.getConnection();
             Statement stmt = conexion.createStatement()) {
            // esquema ya validado, DROP SCHEMA CASCADE es el rollback de todo el provisionamiento
            stmt.execute("DROP SCHEMA IF EXISTS " + esquema + " CASCADE");
            log.warn("Esquema {} eliminado como rollback de provisionamiento fallido", esquema);
        } catch (Exception ex) {
            log.error("No se pudo limpiar el esquema {} tras fallo: {}", esquema, ex.getMessage());
        }
    }

    private String generarPassword() {
        return SECURE_RANDOM.ints(12, 0, CHARS_PASSWORD.length())
                .mapToObj(i -> String.valueOf(CHARS_PASSWORD.charAt(i)))
                .collect(Collectors.joining());
    }
}
