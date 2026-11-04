
package br.com.freyja.service;

import br.com.freyja.dto.AtualizarLancamentoRequestDTO;
import br.com.freyja.dto.PagamentoRequestDTO;
import br.com.freyja.exception.BusinessConflictException;
import br.com.freyja.model.Categoria;
import br.com.freyja.model.Grupo;
import br.com.freyja.model.Lancamento;
import br.com.freyja.repository.CategoriaRepository;
import br.com.freyja.repository.GrupoRepository;
import br.com.freyja.repository.LancamentoRepository;

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
class LancamentoServiceTest {

    @Mock
    private LancamentoRepository lancamentoRepository;

    @Mock
    private GrupoRepository grupoRepository;

    @Mock
    private CategoriaRepository categoriaRepository;

    @InjectMocks
    private LancamentoService service;

    private Lancamento lancamento;


    @BeforeEach
    void setUp() {
        Grupo grupo = new Grupo();
        grupo.setId(1L);
        grupo.setNome("Casa");

        Categoria categoria = new Categoria();
        categoria.setId(1L);
        categoria.setNome("Veículos");

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
    void naoDeveEditarLancamentoPago() {
        lancamento.setStatus(Lancamento.StatusLancamento.PAGO);

        AtualizarLancamentoRequestDTO request =
                new AtualizarLancamentoRequestDTO(
                        "Novo nome",
                        null,
                        new BigDecimal("400.00"),
                        null,
                        null
                );

        when(lancamentoRepository.findById(4L))
                .thenReturn(Optional.of(lancamento));

        assertThrows(
                BusinessConflictException.class,
                () -> service.atualizar(4L, 0L, request)
        );

        verify(lancamentoRepository, never()).saveAndFlush(any());

        assertEquals(
                new BigDecimal("350.00"),
                lancamento.getValorPrevisto()
        );
    }

    @Test
    void naoDeveCancelarLancamentoPago() {
        lancamento.setStatus(Lancamento.StatusLancamento.PAGO);

        when(lancamentoRepository.findById(4L))
                .thenReturn(Optional.of(lancamento));

        assertThrows(
                BusinessConflictException.class,
                () -> service.cancelar(4L, 0L)
        );

        verify(lancamentoRepository, never()).saveAndFlush(any());

        assertEquals(
                Lancamento.StatusLancamento.PAGO,
                lancamento.getStatus()
        );
    }

    @Test
    void deveCancelarLancamentoPendente() {
        when(lancamentoRepository.findById(4L))
                .thenReturn(Optional.of(lancamento));

        when(lancamentoRepository.saveAndFlush(any(Lancamento.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.cancelar(4L, 0L);

        assertEquals(
                Lancamento.StatusLancamento.CANCELADO,
                lancamento.getStatus()
        );

        verify(lancamentoRepository).saveAndFlush(lancamento);
        verify(lancamentoRepository, never()).delete(any());
    }


    @Test
    void naoDeveEditarLancamentoComVersaoDesatualizada() {
        lancamento.setVersion(2L);

        AtualizarLancamentoRequestDTO request =
                new AtualizarLancamentoRequestDTO(
                        "Novo nome",
                        null,
                        new BigDecimal("400.00"),
                        null,
                        null
                );

        when(lancamentoRepository.findById(4L))
                .thenReturn(Optional.of(lancamento));

        BusinessConflictException exception = assertThrows(
                BusinessConflictException.class,
                () -> service.atualizar(4L, 1L, request)
        );

        assertTrue(
                exception.getMessage().contains("alterado")
        );

        verify(lancamentoRepository, never()).saveAndFlush(any());

        assertEquals(
                new BigDecimal("350.00"),
                lancamento.getValorPrevisto()
        );

        assertEquals(2L, lancamento.getVersion());
    }


    @Test
    void naoDeveCancelarComVersaoDesatualizada() {
        lancamento.setVersion(2L);

        when(lancamentoRepository.findById(4L))
                .thenReturn(Optional.of(lancamento));

        assertThrows(
                BusinessConflictException.class,
                () -> service.cancelar(4L, 1L)
        );

        assertEquals(
                Lancamento.StatusLancamento.PENDENTE,
                lancamento.getStatus()
        );

        verify(lancamentoRepository, never()).saveAndFlush(any());
    }


    @Test
    void naoDevePagarComVersaoDesatualizada() {
        lancamento.setVersion(2L);

        PagamentoRequestDTO request = new PagamentoRequestDTO(
                new BigDecimal("350.00"),
                LocalDate.of(2026, 11, 15)
        );

        when(lancamentoRepository.findById(4L))
                .thenReturn(Optional.of(lancamento));

        assertThrows(
                BusinessConflictException.class,
                () -> service.registrarPagamento(4L, 1L, request)
        );

        assertEquals(
                Lancamento.StatusLancamento.PENDENTE,
                lancamento.getStatus()
        );

        assertNull(lancamento.getValorPago());
        assertNull(lancamento.getDataPagamento());

        verify(lancamentoRepository, never()).saveAndFlush(any());
    }



}
