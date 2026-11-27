
package br.com.freyja.integration;

import br.com.freyja.dto.lancamento.LancamentoResponseDTO;
import br.com.freyja.dto.pagamento.EstornarPagamentoRequestDTO;
import br.com.freyja.dto.pagamento.RegistrarPagamentoRequestDTO;

import br.com.freyja.model.Categoria;
import br.com.freyja.model.Grupo;
import br.com.freyja.model.Lancamento;
import br.com.freyja.model.Pagamento;

import br.com.freyja.repository.CategoriaRepository;
import br.com.freyja.repository.GrupoRepository;
import br.com.freyja.repository.LancamentoRepository;
import br.com.freyja.repository.PagamentoRepository;

import br.com.freyja.service.PagamentoService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest
class PagamentoEstornoIntegrationTest {

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
    private PagamentoService pagamentoService;

    @Test
    void deveEstornarPagamentoEManterHistoricoNoPostgres() {

        // 1. Criar os dados no banco temporario
        Grupo grupo = new Grupo();
        grupo.setNome("Casa - teste estorno");
        grupo = grupoRepository.save(grupo);

        Categoria categoria = new Categoria();
        categoria.setNome("Despesas - teste estorno");
        categoria = categoriaRepository.save(categoria);

        Lancamento lancamento = new Lancamento();
        lancamento.setGrupo(grupo);
        lancamento.setCategoria(categoria);
        lancamento.setDescricao("Despesa de teste estorno");
        lancamento.setCompetencia(LocalDate.of(2026, 11, 1));
        lancamento.setVencimento(LocalDate.of(2026, 11, 20));
        lancamento.setValorPrevisto(new BigDecimal("350.00"));
        lancamento.setStatus(Lancamento.StatusLancamento.PENDENTE);

        lancamento = lancamentoRepository.saveAndFlush(lancamento);

        Long lancamentoId = lancamento.getId();
        Long versaoInicial = lancamento.getVersion();

        // 2. Primeiro pagamento: R$ 100
        RegistrarPagamentoRequestDTO primeiroRequest =
                new RegistrarPagamentoRequestDTO(
                        new BigDecimal("100.00"),
                        LocalDate.of(2026, 11, 15)
                );

        LancamentoResponseDTO aposPrimeiro =
                pagamentoService.registrarPagamentoParcial(
                        lancamentoId,
                        versaoInicial,
                        primeiroRequest,
                        UUID.randomUUID().toString()
                );

        assertEquals(
                "PARCIAL",
                aposPrimeiro.status()
        );

        // 3. Segundo pagamento: R$ 250
        RegistrarPagamentoRequestDTO segundoRequest =
                new RegistrarPagamentoRequestDTO(
                        new BigDecimal("250.00"),
                        LocalDate.of(2026, 11, 20)
                );

        LancamentoResponseDTO aposSegundo =
                pagamentoService.registrarPagamentoParcial(
                        lancamentoId,
                        aposPrimeiro.version(),
                        segundoRequest,
                        UUID.randomUUID().toString()
                );

        assertEquals(
                "PAGO",
                aposSegundo.status()
        );

        assertEquals(
                0,
                new BigDecimal("350.00")
                        .compareTo(aposSegundo.valorPago())
        );

        // 4. Localizar o pagamento de R$ 250
        List<Pagamento> historicoAntes =
                pagamentoRepository
                        .findByLancamento_IdOrderByDataPagamentoAscIdAsc(
                                lancamentoId
                        );

        assertEquals(2, historicoAntes.size());

        Pagamento segundoPagamento = historicoAntes.stream()
                .filter(p -> p.getValor()
                        .compareTo(new BigDecimal("250.00")) == 0)
                .findFirst()
                .orElseThrow();

        Long pagamentoId = segundoPagamento.getId();

        // 5. Executar o estorno real
        EstornarPagamentoRequestDTO estornoRequest =
                new EstornarPagamentoRequestDTO(
                        "Pagamento registrado por engano"
                );

        LancamentoResponseDTO aposEstorno =
                pagamentoService.estornarPagamento(
                        lancamentoId,
                        pagamentoId,
                        aposSegundo.version(),
                        estornoRequest
                );

        // 6. Validar resposta do Service
        assertEquals(
                "PARCIAL",
                aposEstorno.status()
        );

        assertEquals(
                0,
                new BigDecimal("100.00")
                        .compareTo(aposEstorno.valorPago())
        );

        assertEquals(
                aposSegundo.version() + 1,
                aposEstorno.version()
        );

        // 7. Consultar novamente o PostgreSQL
        Lancamento salvo = lancamentoRepository
                .findById(lancamentoId)
                .orElseThrow();

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
                LocalDate.of(2026, 11, 15),
                salvo.getDataPagamento()
        );

        assertEquals(
                aposEstorno.version(),
                salvo.getVersion()
        );

        // 8. Confirmar que o historico foi preservado
        List<Pagamento> historicoDepois =
                pagamentoRepository
                        .findByLancamento_IdOrderByDataPagamentoAscIdAsc(
                                lancamentoId
                        );

        assertEquals(
                2,
                historicoDepois.size(),
                "O estorno nao deve excluir pagamentos"
        );

        Pagamento estornado = pagamentoRepository
                .findById(pagamentoId)
                .orElseThrow();

        assertTrue(estornado.isEstornado());
        assertNotNull(estornado.getEstornadoEm());

        assertEquals(
                "Pagamento registrado por engano",
                estornado.getMotivoEstorno()
        );

        // 9. Apenas pagamentos validos entram na soma
        BigDecimal totalValido =
                pagamentoRepository.somarPagamentosPorLancamento(
                        lancamentoId
                );

        assertEquals(
                0,
                new BigDecimal("100.00").compareTo(totalValido)
        );

        long quantidadeValidos = historicoDepois.stream()
                .filter(p -> !p.isEstornado())
                .count();

        assertEquals(1L, quantidadeValidos);
    }
}
