
package br.com.freyja.integration;

import br.com.freyja.model.Categoria;
import br.com.freyja.model.Grupo;
import br.com.freyja.model.Lancamento;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.OptimisticLockException;

import org.hibernate.StaleObjectStateException;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest
class LancamentoConcorrenciaIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("freyja_test")
                    .withUsername("postgres")
                    .withPassword("postgres");

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void deveImpedirAtualizacaoConcorrente() {

        Long id = criarLancamentoTemporario();

        EntityManager emA = entityManagerFactory.createEntityManager();
        EntityManager emB = entityManagerFactory.createEntityManager();

        try {
            emA.getTransaction().begin();
            emB.getTransaction().begin();

            Lancamento lancamentoA = emA.find(Lancamento.class, id);
            Lancamento lancamentoB = emB.find(Lancamento.class, id);

            assertNotNull(lancamentoA);
            assertNotNull(lancamentoB);

            Long versaoInicial = lancamentoA.getVersion();

            assertEquals(
                    versaoInicial,
                    lancamentoB.getVersion()
            );

            // Transacao A realiza a primeira alteracao.
            lancamentoA.setObservacoes(
                    "Alterado pela transacao A"
            );

            emA.getTransaction().commit();

            // Transacao B continua com a versao antiga.
            lancamentoB.setObservacoes(
                    "Alterado pela transacao B"
            );

            RuntimeException exception = assertThrows(
                    RuntimeException.class,
                    () -> emB.getTransaction().commit()
            );

            assertTrue(
                    possuiConflitoOtimista(exception),
                    "Era esperado um conflito de versao"
            );

            // Verificar os dados persistidos.
            EntityManager verificacao =
                    entityManagerFactory.createEntityManager();

            try {
                Lancamento salvo =
                        verificacao.find(Lancamento.class, id);

                assertNotNull(salvo);

                assertEquals(
                        "Alterado pela transacao A",
                        salvo.getObservacoes()
                );

                assertEquals(
                        versaoInicial + 1,
                        salvo.getVersion()
                );

            } finally {
                verificacao.close();
            }

        } finally {
            if (emA.getTransaction().isActive()) {
                emA.getTransaction().rollback();
            }

            if (emB.getTransaction().isActive()) {
                emB.getTransaction().rollback();
            }

            emA.close();
            emB.close();
        }
    }

    private Long criarLancamentoTemporario() {

        EntityManager em =
                entityManagerFactory.createEntityManager();

        try {
            em.getTransaction().begin();

            Grupo grupo = new Grupo();
            grupo.setNome("Casa - teste concorrencia");
            em.persist(grupo);

            Categoria categoria = new Categoria();
            categoria.setNome("Categoria - teste concorrencia");
            em.persist(categoria);

            Lancamento lancamento = new Lancamento();

            lancamento.setGrupo(grupo);
            lancamento.setCategoria(categoria);
            lancamento.setContaRecorrente(null);

            lancamento.setDescricao(
                    "Lancamento temporario - concorrencia"
            );

            lancamento.setCompetencia(
                    LocalDate.of(2026, 11, 1)
            );

            lancamento.setVencimento(
                    LocalDate.of(2026, 11, 20)
            );

            lancamento.setValorPrevisto(
                    new BigDecimal("350.00")
            );

            lancamento.setStatus(
                    Lancamento.StatusLancamento.PENDENTE
            );

            em.persist(lancamento);

            em.getTransaction().commit();

            return lancamento.getId();

        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }

            em.close();
        }
    }

    private boolean possuiConflitoOtimista(Throwable erro) {

        Throwable atual = erro;

        while (atual != null) {

            if (atual instanceof OptimisticLockException
                    || atual instanceof StaleObjectStateException) {
                return true;
            }

            atual = atual.getCause();
        }

        return false;
    }
}
