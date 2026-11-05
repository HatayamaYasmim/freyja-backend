package br.com.freyja.service;

import br.com.freyja.dto.relatorio.ResumoAnualDTO;
import br.com.freyja.dto.relatorio.ResumoFinanceiroDTO;
import br.com.freyja.model.Lancamento;
import br.com.freyja.repository.LancamentoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RelatorioFinanceiroService {
    //Resumo mensal, resumo anual e cálculo dos indicadores financeiros.

    private final LancamentoRepository lancamentoRepository;

    public RelatorioFinanceiroService(
            LancamentoRepository lancamentoRepository
    ) {
        this.lancamentoRepository = lancamentoRepository;
    }


    @Transactional(readOnly = true)
    public ResumoFinanceiroDTO obterResumo(
            Long grupoId,
            YearMonth mes
    ) {
        validarFiltro(grupoId, mes);

        List<Lancamento> lancamentos = lancamentoRepository
                .findByGrupo_IdAndCompetenciaBetween(
                        grupoId,
                        mes.atDay(1),
                        mes.atEndOfMonth()
                );

        return calcularResumo(grupoId, mes, lancamentos);
    }

    @Transactional(readOnly = true)
    public ResumoAnualDTO obterResumoAnual(Long grupoId, Integer ano) {
        if (grupoId == null || grupoId <= 0) {
            throw new IllegalArgumentException(
                    "O ID do grupo deve ser positivo"
            );
        }

        if (ano == null || ano < 2000 || ano > 2100) {
            throw new IllegalArgumentException(
                    "O ano informado é inválido"
            );
        }

        List<Lancamento> lancamentos = lancamentoRepository
                .findByGrupo_IdAndCompetenciaBetween(
                        grupoId,
                        LocalDate.of(ano, 1, 1),
                        LocalDate.of(ano, 12, 31)
                );

        Map<YearMonth, List<Lancamento>> porMes =
                lancamentos.stream()
                        .collect(Collectors.groupingBy(
                                lancamento -> YearMonth.from(
                                        lancamento.getCompetencia()
                                )
                        ));

        List<ResumoFinanceiroDTO> meses = new ArrayList<>();

        BigDecimal totalPrevisto = BigDecimal.ZERO;
        BigDecimal totalPago = BigDecimal.ZERO;
        BigDecimal totalPendente = BigDecimal.ZERO;

        for (int numeroMes = 1; numeroMes <= 12; numeroMes++) {
            YearMonth mes = YearMonth.of(ano, numeroMes);

            List<Lancamento> lancamentosDoMes =
                    porMes.getOrDefault(mes, List.of());

            ResumoFinanceiroDTO resumo =
                    calcularResumo(grupoId, mes, lancamentosDoMes);

            meses.add(resumo);

            totalPrevisto = totalPrevisto.add(resumo.totalPrevisto());
            totalPago = totalPago.add(resumo.totalPago());
            totalPendente = totalPendente.add(resumo.totalPendente());
        }

        return new ResumoAnualDTO(
                grupoId,
                ano,
                totalPrevisto,
                totalPago,
                totalPendente,
                meses
        );
    }


    private ResumoFinanceiroDTO calcularResumo(
            Long grupoId,
            YearMonth mes,
            List<Lancamento> lancamentos
    ) {
        BigDecimal totalPrevisto = BigDecimal.ZERO;
        BigDecimal totalPago = BigDecimal.ZERO;
        BigDecimal totalPendente = BigDecimal.ZERO;

        long quantidadePendentes = 0;
        long quantidadePagos = 0;

        for (Lancamento lancamento : lancamentos) {

            if (lancamento.getStatus()
                    == Lancamento.StatusLancamento.CANCELADO) {
                continue;
            }

            BigDecimal previsto = lancamento.getValorPrevisto();

            BigDecimal pago = lancamento.getValorPago() != null
                    ? lancamento.getValorPago()
                    : BigDecimal.ZERO;

            BigDecimal pendente = previsto.subtract(pago);

            totalPrevisto = totalPrevisto.add(previsto);
            totalPago = totalPago.add(pago);
            totalPendente = totalPendente.add(pendente);

            if (lancamento.getStatus()
                    == Lancamento.StatusLancamento.PAGO) {
                quantidadePagos++;
            } else {
                quantidadePendentes++;
            }
        }

        return new ResumoFinanceiroDTO(
                grupoId,
                mes,
                totalPrevisto,
                totalPago,
                totalPendente,
                quantidadePendentes,
                quantidadePagos
        );
    }


    private void validarFiltro(Long grupoId, YearMonth mes) {
        if (grupoId == null || grupoId <= 0) {
            throw new IllegalArgumentException(
                    "O ID do grupo deve ser positivo"
            );
        }

        if (mes == null) {
            throw new IllegalArgumentException(
                    "O mês é obrigatório"
            );
        }
    }

}
