
package br.com.freyja.integration;

import br.com.freyja.model.Categoria;
import br.com.freyja.model.Grupo;
import br.com.freyja.model.Lancamento;
import br.com.freyja.model.Pagamento;

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
class PagamentoConcorrenciaIntegrationTest {

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
    void naoDeveDuplicarPagamentoEmTransacoesConcorrentes() {

        Long id = criarLancamentoTemporario();

        EntityManager emA = entityManagerFactory.createEntityManager();
        EntityManager emB = entityManagerFactory.createEntityManager();

        try {
            emA.getTransaction().begin();
            emB.getTransaction().begin();

            Lancamento lancamentoA = emA.find(Lancamento.class, id);
            Lancamento lancamentoB = emB.find(Lancamento.class, id);

            assertEquals(0L, lancamentoA.getVersion());
            assertEquals(
                    lancamentoA.getVersion(),
                    lancamentoB.getVersion()
            );

            registrarPagamento(emA, lancamentoA);

            // Primeira transacao confirma o pagamento.
            emA.getTransaction().commit();

            registrarPagamento(emB, lancamentoB);

            // Segunda transacao tenta salvar com a versao antiga.
            RuntimeException exception = assertThrows(
                    RuntimeException.class,
                    () -> emB.getTransaction().commit()
            );

            assertTrue(
                    possuiConflitoOtimista(exception),
                    "Esperado conflito otimista na transacao B"
            );

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

        // Verificar em um novo contexto de persistencia.
        EntityManager verificacao =
                entityManagerFactory.createEntityManager();

        try {
            Lancamento salvo =
                    verificacao.find(Lancamento.class, id);

            assertEquals(
                    Lancamento.StatusLancamento.PAGO,
                    salvo.getStatus()
            );

            assertEquals(
                    0,
                    new BigDecimal("100.00")
                            .compareTo(salvo.getValorPago())
            );

            assertEquals(1L, salvo.getVersion());

            Long quantidade = verificacao.createQuery(
                            """
                            SELECT COUNT(p)
                            FROM Pagamento p
                            WHERE p.lancamento.id = :id
                            """,
                            Long.class
                    )
                    .setParameter("id", id)
                    .getSingleResult();

            assertEquals(1L, quantidade);

            BigDecimal total = verificacao.createQuery(
                            """
                            SELECT SUM(p.valor)
                            FROM Pagamento p
                            WHERE p.lancamento.id = :id
                            """,
                            BigDecimal.class
                    )
                    .setParameter("id", id)
                    .getSingleResult();

            assertEquals(
                    0,
                    new BigDecimal("100.00").compareTo(total)
            );

        } finally {
            verificacao.close();
        }
    }

    private void registrarPagamento(
            EntityManager em,
            Lancamento lancamento
    ) {
        Pagamento pagamento = new Pagamento();
        pagamento.setLancamento(lancamento);
        pagamento.setValor(new BigDecimal("100.00"));
        pagamento.setDataPagamento(LocalDate.of(2026, 11, 15));

        em.persist(pagamento);

        lancamento.setValorPago(new BigDecimal("100.00"));
        lancamento.setDataPagamento(LocalDate.of(2026, 11, 15));

        lancamento.setStatus(
                Lancamento.StatusLancamento.PAGO
        );
    }

    private Long criarLancamentoTemporario() {
        EntityManager em = entityManagerFactory.createEntityManager();

        try {
            em.getTransaction().begin();

            Grupo grupo = new Grupo();
            grupo.setNome("Grupo teste concorrencia");
            em.persist(grupo);

            Categoria categoria = new Categoria();
            categoria.setNome("Categoria teste concorrencia");
            em.persist(categoria);

            Lancamento lancamento = new Lancamento();
            lancamento.setGrupo(grupo);
            lancamento.setCategoria(categoria);
            lancamento.setDescricao("Teste pagamento concorrente");
            lancamento.setCompetencia(LocalDate.of(2026, 11, 1));
            lancamento.setVencimento(LocalDate.of(2026, 11, 15));
            lancamento.setValorPrevisto(new BigDecimal("100.00"));
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
