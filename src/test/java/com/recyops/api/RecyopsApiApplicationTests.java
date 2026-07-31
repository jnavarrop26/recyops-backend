package com.recyops.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

/**
 * Contexto levantado contra un Postgres real en Docker (Testcontainers), nunca
 * contra el Supabase de produccion. MigradorEsquemas lee spring.datasource.*
 * directo por @Value (bypasea el DataSource inyectado a proposito, ver su
 * javadoc), asi que @DynamicPropertySource sobreescribe esas mismas claves en
 * vez de usar @ServiceConnection, que solo registra un JdbcConnectionDetails
 * y no cubre ese caso.
 */
@SpringBootTest
@Testcontainers
@TestPropertySource(properties = {
		"supabase.url=http://localhost:54321",
		"supabase.anon-key=test-anon-key",
		"supabase.service-role-key=test-service-role-key"
})
class RecyopsApiApplicationTests {

	@Container
	static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

	@DynamicPropertySource
	static void datasource(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
		registry.add("spring.datasource.username", POSTGRES::getUsername);
		registry.add("spring.datasource.password", POSTGRES::getPassword);
	}

	@Test
	void contextLoads() {
	}

}
