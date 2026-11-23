
package br.com.freyja.integration;

import br.com.freyja.dto.pagamento.RegistrarPagamentoRequestDTO;
import br.com.freyja.model.Categoria;
import br.com.freyja.model.Grupo;
import br.com.freyja.model.Lancamento;
import br.com.freyja.repository.CategoriaRepository;
import br.com.freyja.repository.GrupoRepository;
import br.com.freyja.repository.LancamentoRepository;
import br.com.freyja.repository.PagamentoRepository;
import br.com.freyja.service.PagamentoService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

@Testcontainers
@SpringBootTest
class PagamentoRollbackIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("freyja_rollback_test")
                    .withUsername("postgres")
                    .withPassword("postgres");

    @Autowired
    private PagamentoService pagamentoService;

    @MockitoSpyBean
    private LancamentoRepository lancamentoRepository;

    @Autowired
    private PagamentoRepository pagamentoRepository;

    @Autowired
    private GrupoRepository grupoRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void deveReverterPagamentoQuandoAtualizacaoDoLancamentoFalhar() {

        // 1. Preparar dados reais no PostgreSQL
        Grupo grupo = new Grupo();
        grupo.setNome("Casa - rollback");
        grupo = grupoRepository.save(grupo);

        Categoria categoria = new Categoria();
        categoria.setNome("Teste rollback");
        categoria = categoriaRepository.save(categoria);

        Lancamento lancamento = new Lancamento();
        lancamento.setGrupo(grupo);
        lancamento.setCategoria(categoria);
        lancamento.setDescricao("Despesa teste rollback");
        lancamento.setCompetencia(LocalDate.of(2026, 11, 1));
        lancamento.setVencimento(LocalDate.of(2026, 11, 20));
        lancamento.setValorPrevisto(new BigDecimal("350.00"));
        lancamento.setStatus(Lancamento.StatusLancamento.PENDENTE);

        lancamento = lancamentoRepository.saveAndFlush(lancamento);

        Long id = lancamento.getId();
        Long versaoInicial = lancamento.getVersion();

        AtomicInteger registrosVisiveisNaTransacao =
                new AtomicInteger(-1);

        // 2. Interceptar a atualização do lançamento
        doAnswer(invocation -> {

            // Força o INSERT do pagamento no PostgreSQL
            pagamentoRepository.flush();

            // Confirma que a transação enxerga o pagamento
            Integer quantidade = jdbcTemplate.queryForObject(
                    """
                    SELECT COUNT(*)
                    FROM pagamento
                    WHERE lancamento_id = ?
                    """,
                    Integer.class,
                    id
            );

            registrosVisiveisNaTransacao.set(quantidade);

            // Falha proposital ANTES do commit
            throw new IllegalStateException(
                    "Falha simulada apos INSERT do pagamento"
            );

        }).when(lancamentoRepository)
                .saveAndFlush(any(Lancamento.class));

        // 3. Executar o pagamento
        RegistrarPagamentoRequestDTO request =
                new RegistrarPagamentoRequestDTO(
                        new BigDecimal("100.00"),
                        LocalDate.of(2026, 11, 15)
                );


        RuntimeException erro = assertThrows(
                RuntimeException.class,
                () -> pagamentoService.registrarPagamentoParcial(
                        id,
                        versaoInicial,
                        request
                )
        );

        Throwable causa = erro;
        boolean encontrouFalhaSimulada = false;

        while (causa != null) {
            if (causa instanceof IllegalStateException
                    && causa.getMessage() != null
                    && causa.getMessage().contains(
                    "Falha simulada apos INSERT do pagamento"
            )) {

                encontrouFalhaSimulada = true;
                break;
            }

            causa = causa.getCause();
        }

        assertTrue(
                encontrouFalhaSimulada,
                "A exceção deveria conter a falha simulada"
        );


        // 4. O pagamento chegou a ser inserido na transação
        assertEquals(1, registrosVisiveisNaTransacao.get());

        // 5. Após o rollback, não existe pagamento persistido
        Long quantidadePersistida = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM pagamento
                WHERE lancamento_id = ?
                """,
                Long.class,
                id
        );

        assertEquals(0L, quantidadePersistida);

        // 6. O lançamento continua inalterado
        Lancamento encontrado = lancamentoRepository.findById(id)
                .orElseThrow();

        assertEquals(
                Lancamento.StatusLancamento.PENDENTE,
                encontrado.getStatus()
        );

        assertNull(encontrado.getValorPago());
        assertNull(encontrado.getDataPagamento());

        assertEquals(
                versaoInicial,
                encontrado.getVersion()
        );
    }
}
