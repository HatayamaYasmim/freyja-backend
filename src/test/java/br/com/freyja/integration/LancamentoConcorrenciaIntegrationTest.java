
package br.com.freyja.integration;

import br.com.freyja.model.Lancamento;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.OptimisticLockException;

import org.hibernate.StaleObjectStateException;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class LancamentoConcorrenciaIntegrationTest {

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

            assertEquals(
                    lancamentoA.getVersion(),
                    lancamentoB.getVersion()
            );

            Long versaoInicial = lancamentoA.getVersion();

            // A primeira transação altera e confirma.
            lancamentoA.setObservacoes("Alterado pela transacao A");
            emA.getTransaction().commit();

            // A segunda ainda possui a versão antiga.
            lancamentoB.setObservacoes("Alterado pela transacao B");

            RuntimeException exception = assertThrows(
                    RuntimeException.class,
                    () -> emB.getTransaction().commit()
            );

            assertTrue(
                    possuiConflitoOtimista(exception),
                    "Era esperado um conflito de versao"
            );

            // Confirma que a alteração de A foi preservada.
            try (EntityManager verificacao =
                         entityManagerFactory.createEntityManager()) {

                Lancamento salvo =
                        verificacao.find(Lancamento.class, id);

                assertEquals(
                        "Alterado pela transacao A",
                        salvo.getObservacoes()
                );

                assertEquals(
                        versaoInicial + 1,
                        salvo.getVersion()
                );
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

            removerLancamentoTemporario(id);
        }
    }

    private Long criarLancamentoTemporario() {
        EntityManager em = entityManagerFactory.createEntityManager();

        try {
            em.getTransaction().begin();

            Lancamento referencia = em.find(Lancamento.class, 1L);

            assertNotNull(
                    referencia,
                    "O lancamento 1 deve existir no banco de desenvolvimento"
            );

            Lancamento novo = new Lancamento();
            novo.setGrupo(referencia.getGrupo());
            novo.setCategoria(referencia.getCategoria());
            novo.setContaRecorrente(null);
            novo.setDescricao("Teste de concorrencia");
            novo.setCompetencia(referencia.getCompetencia());
            novo.setVencimento(referencia.getVencimento());
            novo.setValorPrevisto(referencia.getValorPrevisto());
            novo.setStatus(Lancamento.StatusLancamento.PENDENTE);

            em.persist(novo);
            em.getTransaction().commit();

            return novo.getId();

        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }

            em.close();
        }
    }

    private void removerLancamentoTemporario(Long id) {
        EntityManager em = entityManagerFactory.createEntityManager();

        try {
            em.getTransaction().begin();

            Lancamento lancamento = em.find(Lancamento.class, id);

            if (lancamento != null) {
                em.remove(lancamento);
            }

            em.getTransaction().commit();

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
