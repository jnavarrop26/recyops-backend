package com.recyops.api.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base comun para tests de integracion: levanta el contexto contra un Postgres
 * real en Docker (Testcontainers), nunca contra el Supabase de produccion.
 * MigradorEsquemas lee spring.datasource.* directo por @Value (bypasea el
 * DataSource inyectado a proposito, ver su javadoc), asi que @DynamicPropertySource
 * sobreescribe esas mismas claves en vez de usar @ServiceConnection, que solo
 * registra un JdbcConnectionDetails y no cubre ese caso.
 *
 * <p>El contenedor se arranca una sola vez por JVM en un bloque estatico
 * (patron "singleton container") y nunca se detiene explicitamente: Ryuk lo
 * limpia al terminar la JVM. Con {@code @Testcontainers}/{@code @Container} el
 * contenedor se para y reinicia entre clases de test, pero el
 * {@code ApplicationContext} de Spring queda cacheado con la URL vieja (puerto
 * ya cerrado) porque {@code @DynamicPropertySource} solo se evalua al crear el
 * contexto, no al reiniciar el contenedor.
 */
@SpringBootTest
@TestPropertySource(properties = {
        "supabase.url=http://localhost:54321",
        "supabase.anon-key=test-anon-key",
        "supabase.service-role-key=test-service-role-key",
        "recyops.migraciones.al-arrancar=false"
})
public abstract class ApiIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
