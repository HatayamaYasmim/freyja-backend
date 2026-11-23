
package br.com.freyja.integration;

import br.com.freyja.dto.lancamento.LancamentoResponseDTO;
import br.com.freyja.dto.pagamento.RegistrarPagamentoRequestDTO;
import br.com.freyja.model.Categoria;
import br.com.freyja.model.Grupo;
import br.com.freyja.model.Lancamento;
import br.com.freyja.model.Pagamento;

import br.com.freyja.repository.CategoriaRepository;
import br.com.freyja.repository.GrupoRepository;
import br.com.freyja.repository.LancamentoRepository;
import br.com.freyja.repository.PagamentoRepository;

import br.com.freyja.service.PagamentoIdempotenciaFacade;
import br.com.freyja.service.PagamentoService;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.support.TransactionTemplate;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest
class PagamentoIdempotenciaIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("freyja_test")
                    .withUsername("postgres")
                    .withPassword("postgres");

    @Autowired
    private GrupoRepository grupoRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private LancamentoRepository lancamentoRepository;

    @Autowired
    private PagamentoRepository pagamentoRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private PagamentoService pagamentoService;

    @Autowired
    private PagamentoIdempotenciaFacade pagamentoIdempotenciaFacade;

    @Test
    void naoDevePermitirDoisPagamentosComMesmaChave() {
        Grupo grupo = new Grupo();
        grupo.setNome("Grupo idempotencia");
        grupo = grupoRepository.save(grupo);

        Categoria categoria = new Categoria();
        categoria.setNome("Categoria idempotencia");
        categoria = categoriaRepository.save(categoria);

        Lancamento lancamento = new Lancamento();
        lancamento.setGrupo(grupo);
        lancamento.setCategoria(categoria);
        lancamento.setDescricao("Teste idempotencia");
        lancamento.setCompetencia(LocalDate.of(2026, 11, 1));
        lancamento.setVencimento(LocalDate.of(2026, 11, 15));
        lancamento.setValorPrevisto(new BigDecimal("350.00"));
        lancamento.setStatus(Lancamento.StatusLancamento.PENDENTE);
        lancamento = lancamentoRepository.saveAndFlush(lancamento);

        Long lancamentoId = lancamento.getId();
        String chave = UUID.randomUUID().toString();

        // Primeira transacao: pagamento confirmado.
        transactionTemplate.executeWithoutResult(status -> {
            Lancamento referencia =
                    lancamentoRepository.findById(lancamentoId)
                            .orElseThrow();

            Pagamento pagamento = new Pagamento();
            pagamento.setLancamento(referencia);
            pagamento.setValor(new BigDecimal("100.00"));
            pagamento.setDataPagamento(LocalDate.of(2026, 11, 15));
            pagamento.setIdempotencyKey(chave);

            pagamentoRepository.saveAndFlush(pagamento);
        });

        // Segunda transacao: duplicidade deve ser rejeitada.
        assertThrows(
                DataIntegrityViolationException.class,
                () -> transactionTemplate.executeWithoutResult(status -> {
                    Lancamento referencia =
                            lancamentoRepository.findById(lancamentoId)
                                    .orElseThrow();

                    Pagamento duplicado = new Pagamento();
                    duplicado.setLancamento(referencia);
                    duplicado.setValor(new BigDecimal("100.00"));
                    duplicado.setDataPagamento(LocalDate.of(2026, 11, 15));
                    duplicado.setIdempotencyKey(chave);

                    pagamentoRepository.saveAndFlush(duplicado);
                })
        );

        // Nova consulta: somente o primeiro pagamento deve existir.
        Long quantidade = pagamentoRepository
                .findByIdempotencyKey(chave)
                .stream()
                .count()
                + 0L;

        assertEquals(1L, quantidade);

        long totalPagamentos = pagamentoRepository
                .findByLancamento_IdOrderByDataPagamentoAscIdAsc(lancamentoId)
                .size();

        assertEquals(1L, totalPagamentos);
    }

    @Test
    void deveReconhecerRetrySemDuplicarPagamento() {
        // 1. Criar dados reais no PostgreSQL temporario
        Grupo grupo = new Grupo();
        grupo.setNome("Grupo retry");
        grupo = grupoRepository.save(grupo);

        Categoria categoria = new Categoria();
        categoria.setNome("Categoria retry");
        categoria = categoriaRepository.save(categoria);

        Lancamento lancamento = new Lancamento();
        lancamento.setGrupo(grupo);
        lancamento.setCategoria(categoria);
        lancamento.setDescricao("Teste retry idempotente");
        lancamento.setCompetencia(LocalDate.of(2026, 11, 1));
        lancamento.setVencimento(LocalDate.of(2026, 11, 15));
        lancamento.setValorPrevisto(new BigDecimal("350.00"));
        lancamento.setStatus(Lancamento.StatusLancamento.PENDENTE);

        lancamento = lancamentoRepository.saveAndFlush(lancamento);

        Long id = lancamento.getId();
        Long versaoInicial = lancamento.getVersion();

        String chave = UUID.randomUUID().toString();

        RegistrarPagamentoRequestDTO request =
                new RegistrarPagamentoRequestDTO(
                        new BigDecimal("100.00"),
                        LocalDate.of(2026, 11, 15)
                );

        // 2. Primeira solicitacao
        pagamentoService.registrarPagamentoParcial(
                id,
                versaoInicial,
                request,
                chave
        );

        Lancamento aposPrimeiroPagamento =
                lancamentoRepository.findById(id).orElseThrow();

        assertEquals(
                Lancamento.StatusLancamento.PARCIAL,
                aposPrimeiroPagamento.getStatus()
        );

        assertEquals(
                versaoInicial + 1,
                aposPrimeiroPagamento.getVersion()
        );

        // 3. Repetir com a MESMA chave e a versao antiga
        assertDoesNotThrow(() ->
                pagamentoService.registrarPagamentoParcial(
                        id,
                        versaoInicial,
                        request,
                        chave
                )
        );

        // 4. Verificar que existe apenas um pagamento
        var pagamentos =
                pagamentoRepository
                        .findByLancamento_IdOrderByDataPagamentoAscIdAsc(id);

        assertEquals(1, pagamentos.size());

        assertEquals(
                0,
                new BigDecimal("100.00")
                        .compareTo(pagamentos.get(0).getValor())
        );

        assertEquals(
                chave,
                pagamentos.get(0).getIdempotencyKey()
        );

        // 5. O lançamento nao pode ter sido modificado pelo retry
        Lancamento aposRetry =
                lancamentoRepository.findById(id).orElseThrow();

        assertEquals(
                versaoInicial + 1,
                aposRetry.getVersion()
        );

        assertEquals(
                Lancamento.StatusLancamento.PARCIAL,
                aposRetry.getStatus()
        );

        assertEquals(
                0,
                new BigDecimal("100.00")
                        .compareTo(aposRetry.getValorPago())
        );
    }

    @Test
    void deveTratarPagamentosConcorrentesComMesmaChave() throws Exception {

        Grupo grupo = new Grupo();
        grupo.setNome("Grupo concorrencia idempotente");
        grupo = grupoRepository.save(grupo);

        Categoria categoria = new Categoria();
        categoria.setNome("Categoria concorrencia idempotente");
        categoria = categoriaRepository.save(categoria);

        Lancamento lancamento = new Lancamento();
        lancamento.setGrupo(grupo);
        lancamento.setCategoria(categoria);
        lancamento.setDescricao("Pagamento concorrente");
        lancamento.setCompetencia(LocalDate.of(2026, 11, 1));
        lancamento.setVencimento(LocalDate.of(2026, 11, 20));
        lancamento.setValorPrevisto(new BigDecimal("350.00"));
        lancamento.setStatus(Lancamento.StatusLancamento.PENDENTE);

        lancamento = lancamentoRepository.saveAndFlush(lancamento);

        Long id = lancamento.getId();
        Long versaoInicial = lancamento.getVersion();

        String chave = UUID.randomUUID().toString();

        RegistrarPagamentoRequestDTO request =
                new RegistrarPagamentoRequestDTO(
                        new BigDecimal("100.00"),
                        LocalDate.of(2026, 11, 15)
                );

        CountDownLatch prontas = new CountDownLatch(2);
        CountDownLatch iniciar = new CountDownLatch(1);

        record Resultado(
                LancamentoResponseDTO resposta,
                RuntimeException erro
        ) {}

        Callable<Resultado> tarefa = () -> {
            prontas.countDown();

            if (!iniciar.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException(
                        "Timeout aguardando inicio das chamadas"
                );
            }

            try {
                LancamentoResponseDTO resposta =
                        pagamentoIdempotenciaFacade.registrar(
                                id,
                                versaoInicial,
                                request,
                                chave
                        );

                return new Resultado(resposta, null);

            } catch (RuntimeException erro) {
                return new Resultado(null, erro);
            }
        };

        ExecutorService executor = Executors.newFixedThreadPool(2);

        Resultado resultadoA;
        Resultado resultadoB;

        try {
            Future<Resultado> futuroA = executor.submit(tarefa);
            Future<Resultado> futuroB = executor.submit(tarefa);

            assertTrue(
                    prontas.await(10, TimeUnit.SECONDS),
                    "As duas threads deveriam estar prontas"
            );

            iniciar.countDown();

            resultadoA = futuroA.get(45, TimeUnit.SECONDS);
            resultadoB = futuroB.get(45, TimeUnit.SECONDS);

        } finally {
            iniciar.countDown();
            executor.shutdownNow();
        }

        List<Pagamento> pagamentos =
                pagamentoRepository
                        .findByLancamento_IdOrderByDataPagamentoAscIdAsc(id);

        assertEquals(
                1,
                pagamentos.size(),
                "Apenas um pagamento deve ser persistido"
        );

        assertEquals(
                chave,
                pagamentos.get(0).getIdempotencyKey()
        );

        assertEquals(
                0,
                new BigDecimal("100.00")
                        .compareTo(pagamentos.get(0).getValor())
        );

        Lancamento salvo =
                lancamentoRepository.findById(id).orElseThrow();

        assertEquals(
                Lancamento.StatusLancamento.PARCIAL,
                salvo.getStatus()
        );

        assertEquals(
                0,
                new BigDecimal("100.00")
                        .compareTo(salvo.getValorPago())
        );

        assertEquals(
                versaoInicial + 1,
                salvo.getVersion()
        );

        assertNull(
                resultadoA.erro(),
                () -> "Requisicao A falhou: " + resultadoA.erro()
        );

        assertNull(
                resultadoB.erro(),
                () -> "Requisicao B falhou: " + resultadoB.erro()
        );

        assertNotNull(resultadoA.resposta());
        assertNotNull(resultadoB.resposta());
    }

}
