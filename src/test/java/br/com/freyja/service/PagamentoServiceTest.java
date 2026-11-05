
package br.com.freyja.service;

import br.com.freyja.dto.lancamento.PagamentoRequestDTO;
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
import java.time.LocalDate;
import java.util.Optional;

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

}
