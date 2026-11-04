
package br.com.freyja.service;

import br.com.freyja.exception.BusinessConflictException;
import br.com.freyja.model.ContaRecorrente;
import br.com.freyja.model.Lancamento;
import br.com.freyja.repository.ContaRecorrenteRepository;
import br.com.freyja.repository.LancamentoRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Service
public class GeracaoLancamentosService {

    private final ContaRecorrenteRepository contaRepository;
    private final LancamentoRepository lancamentoRepository;

    public GeracaoLancamentosService(
            ContaRecorrenteRepository contaRepository,
            LancamentoRepository lancamentoRepository
    ) {
        this.contaRepository = contaRepository;
        this.lancamentoRepository = lancamentoRepository;
    }

    @Transactional
    public List<Lancamento> gerar(
            Long contaRecorrenteId,
            YearMonth inicio,
            YearMonth fim
    ) {
        if (inicio == null || fim == null || fim.isBefore(inicio)) {
            throw new IllegalArgumentException(
                    "Período de geração inválido"
            );
        }

        if (inicio.plusMonths(23).isBefore(fim)) {
            throw new IllegalArgumentException(
                    "O período máximo é de 24 meses"
            );
        }

        ContaRecorrente conta = contaRepository
                .findById(contaRecorrenteId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Conta recorrente não encontrada"
                        )
                );

        if (!conta.isAtiva()) {
            throw new BusinessConflictException(
                    "Não é possível gerar lançamentos de uma conta recorrente desativada"
            );
        }

        List<Lancamento> gerados = new ArrayList<>();

        for (YearMonth mes = inicio;
             !mes.isAfter(fim);
             mes = mes.plusMonths(1)) {

            if (!deveGerar(conta, mes)) {
                continue;
            }

            LocalDate competencia = mes.atDay(1);

            boolean existe = lancamentoRepository
                    .existsByContaRecorrente_IdAndCompetencia(
                            conta.getId(),
                            competencia
                    );

            if (existe) {
                continue;
            }

            int diaVencimento = Math.min(
                    conta.getDiaVencimento(),
                    mes.lengthOfMonth()
            );

            Lancamento lancamento = new Lancamento();

            lancamento.setContaRecorrente(conta);
            lancamento.setGrupo(conta.getGrupo());
            lancamento.setCategoria(conta.getCategoria());
            lancamento.setDescricao(conta.getDescricao());
            lancamento.setCompetencia(competencia);
            lancamento.setVencimento(
                    mes.atDay(diaVencimento)
            );
            lancamento.setValorPrevisto(conta.getValorPrevisto());
            lancamento.setStatus(
                    Lancamento.StatusLancamento.PENDENTE
            );

            gerados.add(lancamento);
        }

        if (gerados.isEmpty()) {
            return List.of();
        }

        return lancamentoRepository.saveAll(gerados);
    }

    private boolean deveGerar(
            ContaRecorrente conta,
            YearMonth mes
    ) {
        YearMonth mesInicio =
                YearMonth.from(conta.getDataInicio());

        if (mes.isBefore(mesInicio)) {
            return false;
        }

        if (conta.getDataFim() != null) {
            YearMonth mesFim =
                    YearMonth.from(conta.getDataFim());

            if (mes.isAfter(mesFim)) {
                return false;
            }
        }

        return switch (conta.getPeriodicidade()) {
            case MENSAL -> true;

            case ANUAL -> mes.getMonthValue()
                    == mesInicio.getMonthValue();
        };
    }

}
