
package br.com.freyja.service;

import br.com.freyja.dto.relatorio.ResumoAnualDTO;
import br.com.freyja.dto.relatorio.ResumoFinanceiroDTO;
import br.com.freyja.model.Lancamento;
import br.com.freyja.repository.LancamentoRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RelatorioFinanceiroServiceTest {

    @Mock
    private LancamentoRepository lancamentoRepository;

    @InjectMocks
    private RelatorioFinanceiroService service;

    private Lancamento criarLancamento(
            String valorPrevisto,
            String valorPago,
            Lancamento.StatusLancamento status,
            LocalDate competencia
    ) {
        Lancamento lancamento = new Lancamento();

        lancamento.setValorPrevisto(new BigDecimal(valorPrevisto));
        lancamento.setValorPago(
                valorPago == null ? null : new BigDecimal(valorPago)
        );
        lancamento.setStatus(status);
        lancamento.setCompetencia(competencia);

        return lancamento;
    }

    @Test
    void deveCalcularResumoMensalComPagamentoParcial() {
        LocalDate competencia = LocalDate.of(2026, 11, 1);

        List<Lancamento> lancamentos = List.of(
                criarLancamento("120.00", "120.00",
                        Lancamento.StatusLancamento.PAGO, competencia),
                criarLancamento("350.00", "100.00",
                        Lancamento.StatusLancamento.PARCIAL, competencia),
                criarLancamento("200.00", null,
                        Lancamento.StatusLancamento.PENDENTE, competencia),
                criarLancamento("80.00", null,
                        Lancamento.StatusLancamento.CANCELADO, competencia)
        );

        when(lancamentoRepository.findByGrupo_IdAndCompetenciaBetween(
                1L,
                LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 30)
        )).thenReturn(lancamentos);

        ResumoFinanceiroDTO resumo =
                service.obterResumo(1L, YearMonth.of(2026, 11));

        assertEquals(0,
                new BigDecimal("670.00").compareTo(resumo.totalPrevisto()));
        assertEquals(0,
                new BigDecimal("220.00").compareTo(resumo.totalPago()));
        assertEquals(0,
                new BigDecimal("450.00").compareTo(resumo.totalPendente()));

        assertEquals(1, resumo.quantidadePagos());
        assertEquals(2, resumo.quantidadePendentes());
    }

    @Test
    void deveCalcularResumoAnualComPagamentoParcial() {
        LocalDate novembro = LocalDate.of(2026, 11, 1);
        LocalDate dezembro = LocalDate.of(2026, 12, 1);

        List<Lancamento> lancamentos = List.of(
                criarLancamento("350.00", "100.00",
                        Lancamento.StatusLancamento.PARCIAL, novembro),
                criarLancamento("200.00", "200.00",
                        Lancamento.StatusLancamento.PAGO, dezembro)
        );

        when(lancamentoRepository.findByGrupo_IdAndCompetenciaBetween(
                1L,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31)
        )).thenReturn(lancamentos);

        ResumoAnualDTO resumo =
                service.obterResumoAnual(1L, 2026);

        assertEquals(12, resumo.meses().size());

        assertEquals(0,
                new BigDecimal("550.00").compareTo(resumo.totalPrevisto()));
        assertEquals(0,
                new BigDecimal("300.00").compareTo(resumo.totalPago()));
        assertEquals(0,
                new BigDecimal("250.00").compareTo(resumo.totalPendente()));
    }

    @Test
    void naoDeveAceitarMesNulo() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.obterResumo(1L, null)
        );

        verifyNoInteractions(lancamentoRepository);
    }

    @Test
    void naoDeveAceitarAnoInvalido() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.obterResumoAnual(1L, 1999)
        );

        verifyNoInteractions(lancamentoRepository);
    }
}
