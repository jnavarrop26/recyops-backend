package com.recyops.api.tenant;

import jakarta.annotation.PostConstruct;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Aplica las migraciones de esquema. Como cada empresa vive en su propio esquema
 * de Postgres, Flyway se ejecuta una vez por tenant y cada uno lleva su propia
 * tabla {@code flyway_schema_history} dentro de su esquema; asi dos empresas
 * pueden ir en versiones distintas sin estorbarse.
 *
 * <p>Hay dos juegos de migraciones:
 * <ul>
 *   <li>{@code db/migration/plataforma} sobre {@code public}: el directorio de
 *       empresas, que es comun a toda la plataforma.</li>
 *   <li>{@code db/migration/tenant} sobre cada esquema de empresa.</li>
 * </ul>
 *
 * <p>{@code baselineOnMigrate} resuelve solo los dos casos que importan: un
 * esquema que ya tenia tablas antes de adoptar Flyway se marca como baseline en
 * V1 (no se re-ejecuta nada encima de datos vivos), mientras que un esquema
 * recien creado esta vacio y recibe V1 y las siguientes con normalidad.
 *
 * <p>Corre en {@code @PostConstruct}, es decir durante el refresh del contexto:
 * termina antes de que el servidor web acepte la primera peticion.
 */
@Component
public class MigradorEsquemas {

    private static final Logger log = LoggerFactory.getLogger(MigradorEsquemas.class);

    private static final String MIGRACIONES_PLATAFORMA = "classpath:db/migration/plataforma";
    private static final String MIGRACIONES_TENANT = "classpath:db/migration/tenant";
    private static final String VERSION_BASELINE = "1";

    private final JdbcTemplate jdbc;
    private final boolean alArrancar;
    private final String esquemaPorDefecto;
    private final String url;
    private final String usuario;
    private final String password;

    public MigradorEsquemas(JdbcTemplate jdbc,
            @Value("${recyops.migraciones.al-arrancar}") boolean alArrancar,
            @Value("${supabase.esquema-por-defecto}") String esquemaPorDefecto,
            @Value("${spring.datasource.url}") String url,
            @Value("${spring.datasource.username}") String usuario,
            @Value("${spring.datasource.password}") String password) {
        this.jdbc = jdbc;
        this.alArrancar = alArrancar;
        this.esquemaPorDefecto = esquemaPorDefecto;
        this.url = url;
        this.usuario = usuario;
        this.password = password;
    }

    @PostConstruct
    void migrarAlArrancar() {
        if (!alArrancar) {
            log.info("Migraciones al arrancar desactivadas (recyops.migraciones.al-arrancar=false)");
            return;
        }
        migrarPlataforma();
        for (String esquema : esquemasDeEmpresa()) {
            migrarEsquemaEmpresa(esquema);
        }
    }

    /**
     * Deja un esquema de empresa en la ultima version. Lo usa tanto el arranque
     * como el provisionamiento de una empresa nueva, que asi recibe la base y
     * todas las migraciones posteriores sin depender de ninguna plantilla.
     */
    public void migrarEsquemaEmpresa(String esquema) {
        if (!ContextoEmpresa.esNombreValido(esquema)) {
            throw new IllegalArgumentException("Nombre de esquema de empresa invalido: " + esquema);
        }
        MigrateResult resultado = flyway(MIGRACIONES_TENANT, esquema).migrate();
        registrar(esquema, resultado);
    }

    private void migrarPlataforma() {
        registrar("public", flyway(MIGRACIONES_PLATAFORMA, "public").migrate());
    }

    private Flyway flyway(String ubicacion, String esquema) {
        // DataSource propio y no el pool de la aplicacion: Flyway cambia el
        // search_path de su conexion, y una conexion devuelta a Hikari con el
        // search_path de otra empresa es justo la fuga entre tenants que este
        // diseno (esquema por empresa, sin RLS) no puede permitirse.
        return Flyway.configure()
                .dataSource(url, usuario, password)
                .locations(ubicacion)
                .schemas(esquema)
                .defaultSchema(esquema)
                .createSchemas(true)
                .baselineOnMigrate(true)
                .baselineVersion(VERSION_BASELINE)
                .baselineDescription("Esquema existente antes de adoptar Flyway")
                .load();
    }

    /**
     * Esquemas a migrar: los registrados en el directorio (incluidos los
     * inactivos, que si no quedarian atrasados al reactivarse) mas el esquema
     * por defecto de desarrollo, que existe sin estar registrado.
     */
    private Set<String> esquemasDeEmpresa() {
        Set<String> esquemas = new LinkedHashSet<>(
                jdbc.queryForList("select schema_name from public.empresas order by fecha_creacion",
                        String.class));
        if (existeEsquema(esquemaPorDefecto) && esquemas.add(esquemaPorDefecto)) {
            log.info("Esquema {} existe pero no esta en public.empresas; se migra igual",
                    esquemaPorDefecto);
        }
        return esquemas;
    }

    private boolean existeEsquema(String esquema) {
        List<String> encontrados = jdbc.queryForList(
                "select schema_name from information_schema.schemata where schema_name = ?",
                String.class, esquema);
        return !encontrados.isEmpty();
    }

    private void registrar(String esquema, MigrateResult resultado) {
        if (resultado.migrationsExecuted == 0) {
            // Sin migraciones aplicadas Flyway deja targetSchemaVersion en null
            log.info("Esquema {} ya estaba al dia (version {})", esquema, resultado.initialSchemaVersion);
        } else {
            log.info("Esquema {}: {} migracion(es) aplicada(s), version {}",
                    esquema, resultado.migrationsExecuted, resultado.targetSchemaVersion);
        }
    }
}
