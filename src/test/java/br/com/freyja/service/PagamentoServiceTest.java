
package br.com.freyja.service;

import br.com.freyja.dto.lancamento.PagamentoRequestDTO;
import br.com.freyja.dto.pagamento.EstornarPagamentoRequestDTO;
import br.com.freyja.dto.pagamento.PagamentoResponseDTO;
import br.com.freyja.dto.pagamento.RegistrarPagamentoRequestDTO;
import br.com.freyja.exception.BusinessConflictException;
import br.com.freyja.model.Categoria;
import br.com.freyja.model.Grupo;
import br.com.freyja.model.Lancamento;
import br.com.freyja.model.Pagamento;
import br.com.freyja.repository.LancamentoRepository;
import br.com.freyja.repository.PagamentoRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PagamentoServiceTest {

    @Mock
    private LancamentoRepository lancamentoRepository;

    @Mock
    private PagamentoRepository pagamentoRepository;

    @InjectMocks
    private PagamentoService pagamentoService;

    private Lancamento lancamento;


    @BeforeEach
    void setUp() {
        Grupo grupo = new Grupo();
        grupo.setId(1L);
        grupo.setNome("Casa");

        Categoria categoria = new Categoria();
        categoria.setId(1L);
        categoria.setNome("Manutenção");

        lancamento = new Lancamento();
        lancamento.setId(4L);
        lancamento.setGrupo(grupo);
        lancamento.setCategoria(categoria);
        lancamento.setDescricao("Manutenção do carro");
        lancamento.setCompetencia(LocalDate.of(2026, 11, 1));
        lancamento.setVencimento(LocalDate.of(2026, 11, 15));

        lancamento.setValorPrevisto(new BigDecimal("350.00"));
        lancamento.setStatus(Lancamento.StatusLancamento.PENDENTE);
        lancamento.setVersion(0L);
    }


    @Test
    void naoDevePagarComVersaoDesatualizada() {
        lancamento.setVersion(2L);

        PagamentoRequestDTO request =
                new PagamentoRequestDTO(
                        new BigDecimal("350.00"),
                        LocalDate.of(2026, 11, 15)
                );

        when(lancamentoRepository.findById(4L))
                .thenReturn(Optional.of(lancamento));

        BusinessConflictException exception =
                assertThrows(
                        BusinessConflictException.class,
                        () -> pagamentoService.registrarPagamento(
                                4L,
                                1L,
                                request
                        )
                );

        assertTrue(
                exception.getMessage().contains("alterado")
        );

        assertEquals(
                Lancamento.StatusLancamento.PENDENTE,
                lancamento.getStatus()
        );

        assertNull(lancamento.getValorPago());
        assertNull(lancamento.getDataPagamento());

        verify(pagamentoRepository, never()).save(any());
        verify(
                lancamentoRepository,
                never()
        ).saveAndFlush(any());
    }


    @Test
    void deveRegistrarPagamentoParcial() {
        RegistrarPagamentoRequestDTO request =
                new RegistrarPagamentoRequestDTO(
                        new BigDecimal("100.00"),
                        LocalDate.of(2026, 11, 15)
                );

        when(lancamentoRepository.findById(4L))
                .thenReturn(Optional.of(lancamento));

        when(pagamentoRepository.somarPagamentosPorLancamento(4L))
                .thenReturn(BigDecimal.ZERO);

        when(lancamentoRepository.saveAndFlush(any(Lancamento.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        pagamentoService.registrarPagamentoParcial(4L, 0L, request);

        assertEquals(
                Lancamento.StatusLancamento.PARCIAL,
                lancamento.getStatus()
        );

        assertEquals(
                0,
                new BigDecimal("100.00")
                        .compareTo(lancamento.getValorPago())
        );

        verify(pagamentoRepository).save(argThat(p ->
                p.getLancamento() == lancamento
                        && p.getValor().compareTo(
                        new BigDecimal("100.00")) == 0
        ));

        verify(lancamentoRepository).saveAndFlush(lancamento);
    }

    @Test
    void deveQuitarLancamentoComSegundoPagamento() {
        lancamento.setStatus(Lancamento.StatusLancamento.PARCIAL);
        lancamento.setValorPago(new BigDecimal("100.00"));
        lancamento.setVersion(1L);

        RegistrarPagamentoRequestDTO request =
                new RegistrarPagamentoRequestDTO(
                        new BigDecimal("250.00"),
                        LocalDate.of(2026, 11, 20)
                );

        when(lancamentoRepository.findById(4L))
                .thenReturn(Optional.of(lancamento));

        when(pagamentoRepository.somarPagamentosPorLancamento(4L))
                .thenReturn(new BigDecimal("100.00"));

        when(lancamentoRepository.saveAndFlush(any(Lancamento.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        pagamentoService.registrarPagamentoParcial(4L, 1L, request);

        assertEquals(
                Lancamento.StatusLancamento.PAGO,
                lancamento.getStatus()
        );

        assertEquals(
                0,
                new BigDecimal("350.00")
                        .compareTo(lancamento.getValorPago())
        );

        verify(pagamentoRepository).save(argThat(p ->
                p.getValor().compareTo(
                        new BigDecimal("250.00")) == 0
        ));

        verify(lancamentoRepository).saveAndFlush(lancamento);
    }

    @Test
    void naoDevePagarAcimaDoSaldo() {
        lancamento.setStatus(Lancamento.StatusLancamento.PARCIAL);
        lancamento.setValorPago(new BigDecimal("100.00"));
        lancamento.setVersion(1L);

        RegistrarPagamentoRequestDTO request =
                new RegistrarPagamentoRequestDTO(
                        new BigDecimal("300.00"),
                        LocalDate.of(2026, 11, 20)
                );

        when(lancamentoRepository.findById(4L))
                .thenReturn(Optional.of(lancamento));

        when(pagamentoRepository.somarPagamentosPorLancamento(4L))
                .thenReturn(new BigDecimal("100.00"));

        assertThrows(
                BusinessConflictException.class,
                () -> pagamentoService.registrarPagamentoParcial(
                        4L, 1L, request
                )
        );

        verify(pagamentoRepository, never()).save(any(Pagamento.class));
        verify(lancamentoRepository, never()).saveAndFlush(any());
    }


    @Test
    void deveListarHistoricoDePagamentos() {
        Pagamento primeiro = new Pagamento();
        primeiro.setId(2L);
        primeiro.setLancamento(lancamento);
        primeiro.setValor(new BigDecimal("100.00"));
        primeiro.setDataPagamento(LocalDate.of(2026, 11, 15));

        Pagamento segundo = new Pagamento();
        segundo.setId(3L);
        segundo.setLancamento(lancamento);
        segundo.setValor(new BigDecimal("250.00"));
        segundo.setDataPagamento(LocalDate.of(2026, 11, 20));

        when(lancamentoRepository.findById(4L))
                .thenReturn(Optional.of(lancamento));

        when(pagamentoRepository
                .findByLancamento_IdOrderByDataPagamentoAscIdAsc(4L))
                .thenReturn(List.of(primeiro, segundo));

        List<PagamentoResponseDTO> historico =
                pagamentoService.listarPagamentos(4L);

        assertEquals(2, historico.size());
        assertEquals(2L, historico.get(0).id());
        assertEquals(3L, historico.get(1).id());

        assertEquals(
                0,
                new BigDecimal("350.00")
                        .compareTo(
                                historico.get(0).valor()
                                        .add(historico.get(1).valor())
                        )
        );

        verify(pagamentoRepository)
                .findByLancamento_IdOrderByDataPagamentoAscIdAsc(4L);
    }


    @Test
    void naoDeveDuplicarPagamentoComMesmaChave() {
        Long lancamentoId = 1L;
        Long versaoOriginal = 0L;
        lancamento.setId(lancamentoId);

        String chave = UUID.randomUUID().toString();

        RegistrarPagamentoRequestDTO request =
                new RegistrarPagamentoRequestDTO(
                        new BigDecimal("100.00"),
                        LocalDate.of(2026, 11, 15)
                );

        Pagamento pagamentoExistente = new Pagamento();
        pagamentoExistente.setLancamento(lancamento);
        pagamentoExistente.setValor(new BigDecimal("100.00"));
        pagamentoExistente.setDataPagamento(
                LocalDate.of(2026, 11, 15)
        );
        pagamentoExistente.setIdempotencyKey(chave);
        assertEquals(
                lancamentoId,
                pagamentoExistente.getLancamento().getId()
        );

        assertEquals(
                0,
                pagamentoExistente.getValor().compareTo(request.valor())
        );

        assertEquals(
                request.dataPagamento(),
                pagamentoExistente.getDataPagamento()
        );


        when(pagamentoRepository.findByIdempotencyKey(chave))
                .thenReturn(Optional.of(pagamentoExistente));

        when(lancamentoRepository.findById(lancamentoId))
                .thenReturn(Optional.of(lancamento));

        pagamentoService.registrarPagamentoParcial(
                lancamentoId,
                versaoOriginal,
                request,
                chave
        );

        verify(pagamentoRepository, never())
                .save(any(Pagamento.class));

        verify(lancamentoRepository, never())
                .saveAndFlush(any(Lancamento.class));
    }

    @Test
    void naoDeveReutilizarChaveComValorDiferente() {
        String chave = UUID.randomUUID().toString();

        RegistrarPagamentoRequestDTO request =
                new RegistrarPagamentoRequestDTO(
                        new BigDecimal("150.00"),
                        LocalDate.of(2026, 11, 15)
                );

        Pagamento existente = new Pagamento();
        existente.setLancamento(lancamento);
        existente.setValor(new BigDecimal("100.00"));
        existente.setDataPagamento(LocalDate.of(2026, 11, 15));
        existente.setIdempotencyKey(chave);

        when(pagamentoRepository.findByIdempotencyKey(chave))
                .thenReturn(Optional.of(existente));

        assertThrows(
                BusinessConflictException.class,
                () -> pagamentoService.registrarPagamentoParcial(
                        4L, 0L, request, chave
                )
        );

        verify(pagamentoRepository, never()).save(any());
        verify(lancamentoRepository, never()).saveAndFlush(any());
    }

    @Test
    void naoDeveReutilizarChaveEmOutroLancamento() {
        String chave = UUID.randomUUID().toString();

        RegistrarPagamentoRequestDTO request =
                new RegistrarPagamentoRequestDTO(
                        new BigDecimal("100.00"),
                        LocalDate.of(2026, 11, 15)
                );

        Pagamento existente = new Pagamento();
        existente.setLancamento(lancamento); // ID 4
        existente.setValor(new BigDecimal("100.00"));
        existente.setDataPagamento(LocalDate.of(2026, 11, 15));
        existente.setIdempotencyKey(chave);

        when(pagamentoRepository.findByIdempotencyKey(chave))
                .thenReturn(Optional.of(existente));

        assertThrows(
                BusinessConflictException.class,
                () -> pagamentoService.registrarPagamentoParcial(
                        5L, 0L, request, chave
                )
        );

        verify(pagamentoRepository, never()).save(any());
        verify(lancamentoRepository, never()).saveAndFlush(any());
    }

    @Test
    void naoDeveReutilizarChaveComDataDiferente() {
        String chave = UUID.randomUUID().toString();

        RegistrarPagamentoRequestDTO request =
                new RegistrarPagamentoRequestDTO(
                        new BigDecimal("100.00"),
                        LocalDate.of(2026, 11, 20)
                );

        Pagamento existente = new Pagamento();
        existente.setLancamento(lancamento);
        existente.setValor(new BigDecimal("100.00"));
        existente.setDataPagamento(LocalDate.of(2026, 11, 15));
        existente.setIdempotencyKey(chave);

        when(pagamentoRepository.findByIdempotencyKey(chave))
                .thenReturn(Optional.of(existente));

        assertThrows(
                BusinessConflictException.class,
                () -> pagamentoService.registrarPagamentoParcial(
                        4L, 0L, request, chave
                )
        );

        verify(pagamentoRepository, never())
                .save(any(Pagamento.class));

        verify(lancamentoRepository, never())
                .saveAndFlush(any(Lancamento.class));
    }


    @Test
    void deveRejeitarChaveIdempotenciaInvalida() {
        RegistrarPagamentoRequestDTO request =
                new RegistrarPagamentoRequestDTO(
                        new BigDecimal("100.00"),
                        LocalDate.of(2026, 11, 15)
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> pagamentoService.registrarPagamentoParcial(
                        4L,
                        0L,
                        request,
                        "chave-invalida"
                )
        );

        verifyNoInteractions(
                pagamentoRepository,
                lancamentoRepository
        );
    }


    @Test
    void deveEstornarPagamentoEAtualizarLancamentoParaParcial() {
        lancamento.setStatus(Lancamento.StatusLancamento.PAGO);
        lancamento.setValorPago(new BigDecimal("350.00"));
        lancamento.setVersion(2L);
        lancamento.setDataPagamento(LocalDate.of(2026, 11, 20));

        Pagamento pagamento = new Pagamento();
        pagamento.setId(3L);
        pagamento.setLancamento(lancamento);
        pagamento.setValor(new BigDecimal("250.00"));
        pagamento.setDataPagamento(LocalDate.of(2026, 11, 20));

        Pagamento pagamentoRestante = new Pagamento();
        pagamentoRestante.setId(2L);
        pagamentoRestante.setLancamento(lancamento);
        pagamentoRestante.setValor(new BigDecimal("100.00"));
        pagamentoRestante.setDataPagamento(LocalDate.of(2026, 11, 15));

        EstornarPagamentoRequestDTO request =
                new EstornarPagamentoRequestDTO(
                        "Pagamento registrado por engano"
                );

        when(lancamentoRepository.findById(4L))
                .thenReturn(Optional.of(lancamento));

        when(pagamentoRepository.findById(3L))
                .thenReturn(Optional.of(pagamento));

        when(pagamentoRepository.somarPagamentosPorLancamento(4L))
                .thenReturn(new BigDecimal("100.00"));

        when(pagamentoRepository
                .findFirstByLancamento_IdAndEstornadoFalseOrderByDataPagamentoDescIdDesc(4L))
                .thenReturn(Optional.of(pagamentoRestante));

        when(lancamentoRepository.saveAndFlush(any(Lancamento.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        pagamentoService.estornarPagamento(
                4L,
                3L,
                2L,
                request
        );

        assertTrue(pagamento.isEstornado());
        assertNotNull(pagamento.getEstornadoEm());
        assertEquals(
                "Pagamento registrado por engano",
                pagamento.getMotivoEstorno()
        );

        assertEquals(
                Lancamento.StatusLancamento.PARCIAL,
                lancamento.getStatus()
        );

        assertEquals(
                0,
                new BigDecimal("100.00")
                        .compareTo(lancamento.getValorPago())
        );

        assertEquals(
                LocalDate.of(2026, 11, 15),
                lancamento.getDataPagamento()
        );

        verify(pagamentoRepository).save(pagamento);
        verify(lancamentoRepository).saveAndFlush(lancamento);
    }


    @Test
    void deveVoltarParaPendenteAoEstornarUltimoPagamento() {
        lancamento.setStatus(Lancamento.StatusLancamento.PAGO);
        lancamento.setValorPago(new BigDecimal("350.00"));
        lancamento.setVersion(1L);
        lancamento.setDataPagamento(LocalDate.of(2026, 11, 15));

        Pagamento pagamento = new Pagamento();
        pagamento.setId(2L);
        pagamento.setLancamento(lancamento);
        pagamento.setValor(new BigDecimal("350.00"));
        pagamento.setDataPagamento(LocalDate.of(2026, 11, 15));

        EstornarPagamentoRequestDTO request =
                new EstornarPagamentoRequestDTO(
                        "Pagamento lançado incorretamente"
                );

        when(lancamentoRepository.findById(4L))
                .thenReturn(Optional.of(lancamento));

        when(pagamentoRepository.findById(2L))
                .thenReturn(Optional.of(pagamento));

        when(pagamentoRepository.somarPagamentosPorLancamento(4L))
                .thenReturn(BigDecimal.ZERO);

        when(lancamentoRepository.saveAndFlush(any(Lancamento.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        pagamentoService.estornarPagamento(
                4L,
                2L,
                1L,
                request
        );

        assertTrue(pagamento.isEstornado());
        assertNotNull(pagamento.getEstornadoEm());

        assertEquals(
                Lancamento.StatusLancamento.PENDENTE,
                lancamento.getStatus()
        );

        assertNull(lancamento.getValorPago());
        assertNull(lancamento.getDataPagamento());

        verify(pagamentoRepository).save(pagamento);
        verify(lancamentoRepository).saveAndFlush(lancamento);

        verify(pagamentoRepository, never())
                .findFirstByLancamento_IdAndEstornadoFalseOrderByDataPagamentoDescIdDesc(
                        anyLong()
                );
    }


    @Test
    void naoDeveEstornarPagamentoJaEstornado() {
        lancamento.setStatus(Lancamento.StatusLancamento.PENDENTE);
        lancamento.setVersion(2L);

        Pagamento pagamento = new Pagamento();
        pagamento.setId(3L);
        pagamento.setLancamento(lancamento);
        pagamento.setValor(new BigDecimal("250.00"));
        pagamento.setDataPagamento(LocalDate.of(2026, 11, 20));
        pagamento.setEstornado(true);
        pagamento.setEstornadoEm(Instant.parse("2026-11-21T12:00:00Z"));
        pagamento.setMotivoEstorno("Estorno anterior");

        EstornarPagamentoRequestDTO request =
                new EstornarPagamentoRequestDTO(
                        "Tentativa de estorno duplicado"
                );

        when(lancamentoRepository.findById(4L))
                .thenReturn(Optional.of(lancamento));

        when(pagamentoRepository.findById(3L))
                .thenReturn(Optional.of(pagamento));

        BusinessConflictException exception = assertThrows(
                BusinessConflictException.class,
                () -> pagamentoService.estornarPagamento(
                        4L,
                        3L,
                        2L,
                        request
                )
        );

        assertTrue(
                exception.getMessage().contains("já foi estornado")
        );

        assertEquals(
                "Estorno anterior",
                pagamento.getMotivoEstorno()
        );

        assertEquals(
                Lancamento.StatusLancamento.PENDENTE,
                lancamento.getStatus()
        );

        verify(pagamentoRepository, never()).saveAndFlush(any());
        verify(lancamentoRepository, never()).saveAndFlush(any());

        verify(pagamentoRepository, never())
                .somarPagamentosPorLancamento(anyLong());
    }



    @Test
    void naoDeveAceitarRetryDePagamentoEstornado() {
        String chave = UUID.randomUUID().toString();

        lancamento.setVersion(2L);

        Pagamento pagamentoExistente = new Pagamento();
        pagamentoExistente.setId(10L);
        pagamentoExistente.setLancamento(lancamento);
        pagamentoExistente.setValor(new BigDecimal("100.00"));
        pagamentoExistente.setDataPagamento(
                LocalDate.of(2026, 11, 20)
        );
        pagamentoExistente.setIdempotencyKey(chave);
        pagamentoExistente.setEstornado(true);
        pagamentoExistente.setEstornadoEm(Instant.now());
        pagamentoExistente.setMotivoEstorno(
                "Pagamento registrado por engano"
        );

        RegistrarPagamentoRequestDTO request =
                new RegistrarPagamentoRequestDTO(
                        new BigDecimal("100.00"),
                        LocalDate.of(2026, 11, 20)
                );

        when(pagamentoRepository.findByIdempotencyKey(chave))
                .thenReturn(Optional.of(pagamentoExistente));

        assertThrows(
                BusinessConflictException.class,
                () -> pagamentoService.registrarPagamentoParcial(
                        4L,
                        1L, // Versao antiga do pedido original
                        request,
                        chave
                )
        );

        verify(pagamentoRepository, never())
                .save(any(Pagamento.class));

        verify(lancamentoRepository, never())
                .saveAndFlush(any(Lancamento.class));
    }





}
