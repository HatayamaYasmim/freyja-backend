package br.com.freyja.service;

import br.com.freyja.exception.BusinessConflictException;
import br.com.freyja.model.Categoria;
import br.com.freyja.model.ContaRecorrente;
import br.com.freyja.model.Grupo;
import br.com.freyja.model.Lancamento;
import br.com.freyja.repository.ContaRecorrenteRepository;
import br.com.freyja.repository.LancamentoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GeracaoLancamentosServiceTest {

    @Mock
    private LancamentoRepository repository;

    @Mock
    private ContaRecorrenteRepository contaRepository;

    @InjectMocks
    private GeracaoLancamentosService service;

    private ContaRecorrente conta;

    @BeforeEach
    void setUp() {
        Grupo grupo = new Grupo();
        grupo.setId(1L);
        grupo.setNome("Casa");

        Categoria categoria = new Categoria();
        categoria.setId(1L);
        categoria.setNome("Internet");

        conta = new ContaRecorrente();
        conta.setId(1L);
        conta.setDescricao("Internet residencial");
        conta.setGrupo(grupo);
        conta.setCategoria(categoria);
        conta.setValorPrevisto(new BigDecimal("120.00"));
        conta.setPeriodicidade(
                ContaRecorrente.Periodicidade.MENSAL
        );
        conta.setDiaVencimento(10);
        conta.setDataInicio(LocalDate.of(2026, 10, 1));
        conta.setAtiva(true);
    }

    @Test
    void gerarTresLancamentosMensais() {
        when(contaRepository.findById(1L))
                .thenReturn(Optional.of(conta));

        when(repository.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<Lancamento> resultado = service.gerar(
                1L,
                YearMonth.of(2026, 10),
                YearMonth.of(2026, 12)
        );

        assertEquals(3, resultado.size());

        assertEquals(
                LocalDate.of(2026, 10, 1),
                resultado.get(0).getCompetencia()
        );

        assertEquals(
                LocalDate.of(2026, 11, 10),
                resultado.get(1).getVencimento()
        );

        assertEquals(
                new BigDecimal("120.00"),
                resultado.get(2).getValorPrevisto()
        );

        assertEquals(
                Lancamento.StatusLancamento.PENDENTE,
                resultado.get(0).getStatus()
        );

        verify(repository).saveAll(anyList());
    }


    @Test
    void deveGerarLancamentosAnuais() {
        conta.setPeriodicidade(
                ContaRecorrente.Periodicidade.ANUAL
        );

        when(contaRepository.findById(1L))
                .thenReturn(Optional.of(conta));

        when(repository.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<Lancamento> resultado = service.gerar(
                1L,
                YearMonth.of(2026, 10),
                YearMonth.of(2027, 12)
        );

        assertEquals(2, resultado.size());

        assertEquals(
                LocalDate.of(2026, 10, 1),
                resultado.get(0).getCompetencia()
        );

        assertEquals(
                LocalDate.of(2027, 10, 1),
                resultado.get(1).getCompetencia()
        );
    }

    @Test
    void naoDeveDuplicarLancamentoExistente() {
        when(contaRepository.findById(1L))
                .thenReturn(Optional.of(conta));

        when(repository
                .existsByContaRecorrente_IdAndCompetencia(
                        1L,
                        LocalDate.of(2026, 10, 1)
                ))
                .thenReturn(true);

        List<Lancamento> resultado = service.gerar(
                1L,
                YearMonth.of(2026, 10),
                YearMonth.of(2026, 10)
        );

        assertTrue(resultado.isEmpty());

        verify(repository, never())
                .saveAll(anyList());
    }


    @Test
    void deveAjustarVencimentoParaFevereiro() {
        conta.setDiaVencimento(31);

        when(contaRepository.findById(1L))
                .thenReturn(Optional.of(conta));

        when(repository.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<Lancamento> resultado = service.gerar(
                1L,
                YearMonth.of(2027, 2),
                YearMonth.of(2027, 2)
        );

        assertEquals(1, resultado.size());

        assertEquals(
                LocalDate.of(2027, 2, 28),
                resultado.get(0).getVencimento()
        );
    }

    @Test
    void naoDeveGerarLancamentosDeContaInativa() {
        conta.setAtiva(false);

        when(contaRepository.findById(1L))
                .thenReturn(Optional.of(conta));

        assertThrows(
                BusinessConflictException.class,
                () -> service.gerar(
                        1L,
                        YearMonth.of(2027, 1),
                        YearMonth.of(2027, 3)
                )
        );

        verify(repository, never()).saveAll(any());
    }

    @Test
    void naoDeveGerarAntesDaDataInicio() {
        when(contaRepository.findById(1L))
                .thenReturn(Optional.of(conta));

        when(repository.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<Lancamento> resultado = service.gerar(
                1L,
                YearMonth.of(2026, 8),
                YearMonth.of(2026, 11)
        );

        assertEquals(2, resultado.size());

        assertEquals(
                LocalDate.of(2026, 10, 1),
                resultado.get(0).getCompetencia()
        );

        assertEquals(
                LocalDate.of(2026, 11, 1),
                resultado.get(1).getCompetencia()
        );
    }

    @Test
    void naoDeveGerarAposDataFim() {
        conta.setDataFim(LocalDate.of(2026, 11, 30));

        when(contaRepository.findById(1L))
                .thenReturn(Optional.of(conta));

        when(repository.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<Lancamento> resultado = service.gerar(
                1L,
                YearMonth.of(2026, 10),
                YearMonth.of(2027, 2)
        );

        assertEquals(2, resultado.size());

        assertEquals(
                LocalDate.of(2026, 11, 1),
                resultado.get(1).getCompetencia()
        );
    }

    @Test
    void naoDeveAceitarPeriodoInvertido() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.gerar(
                        1L,
                        YearMonth.of(2026, 12),
                        YearMonth.of(2026, 10)
                )
        );

        verifyNoInteractions(contaRepository, repository);
    }

    @Test
    void naoDeveAceitarPeriodoMaiorQue24Meses() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.gerar(
                        1L,
                        YearMonth.of(2026, 1),
                        YearMonth.of(2028, 1)
                )
        );

        verifyNoInteractions(contaRepository, repository);
    }


    @Test
    void deveGerarNoMesDeEncerramento() {
        conta.setDataFim(LocalDate.of(2027, 2, 15));

        when(contaRepository.findById(1L))
                .thenReturn(Optional.of(conta));

        when(repository.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<Lancamento> resultado = service.gerar(
                1L,
                YearMonth.of(2027, 1),
                YearMonth.of(2027, 4)
        );

        assertEquals(2, resultado.size());

        assertEquals(
                LocalDate.of(2027, 1, 1),
                resultado.get(0).getCompetencia()
        );

        assertEquals(
                LocalDate.of(2027, 2, 1),
                resultado.get(1).getCompetencia()
        );

        verify(repository).saveAll(anyList());
    }



}
