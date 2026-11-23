
package br.com.freyja.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import br.com.freyja.service.PagamentoService;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest
class PagamentoIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("freyja_test")
                    .withUsername("postgres")
                    .withPassword("postgres");

    @DynamicPropertySource
    static void configurarBanco(DynamicPropertyRegistry registry) {
        registry.add(
                "spring.datasource.url",
                postgres::getJdbcUrl
        );
        registry.add(
                "spring.datasource.username",
                postgres::getUsername
        );
        registry.add(
                "spring.datasource.password",
                postgres::getPassword
        );

        registry.add(
                "spring.flyway.url",
                postgres::getJdbcUrl
        );
        registry.add(
                "spring.flyway.user",
                postgres::getUsername
        );
        registry.add(
                "spring.flyway.password",
                postgres::getPassword
        );
    }

    @Autowired
    private PagamentoService pagamentoService;

    @Test
    void contextoDeveCarregarComPostgresIsolado() {
        assertNotNull(pagamentoService);
        assertTrue(postgres.isRunning());
    }
}
