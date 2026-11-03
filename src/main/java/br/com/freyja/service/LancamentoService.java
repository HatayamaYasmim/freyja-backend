
package br.com.freyja.service;

import br.com.freyja.dto.LancamentoResponseDTO;
import br.com.freyja.dto.PagamentoRequestDTO;
import br.com.freyja.dto.ResumoFinanceiroDTO;
import br.com.freyja.model.Lancamento;
import br.com.freyja.repository.LancamentoRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.List;
import java.math.BigDecimal;

@Service
public class LancamentoService {

    private final LancamentoRepository lancamentoRepository;

    public LancamentoService(
            LancamentoRepository lancamentoRepository
    ) {
        this.lancamentoRepository = lancamentoRepository;
    }

    @Transactional(readOnly = true)
    public List<LancamentoResponseDTO> listarPorGrupo(
            Long grupoId,
            YearMonth mes
    ) {
        validarFiltro(grupoId, mes);

        return lancamentoRepository
                .findByGrupo_IdAndCompetenciaBetween(
                        grupoId,
                        mes.atDay(1),
                        mes.atEndOfMonth()
                )
                .stream()
                .map(LancamentoResponseDTO::fromEntity)
                .toList();
    }


    @Transactional
    public LancamentoResponseDTO registrarPagamento(
            Long lancamentoId,
            PagamentoRequestDTO request
    ) {
        Lancamento lancamento = lancamentoRepository.findById(lancamentoId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Lançamento não encontrado: " + lancamentoId
                        )
                );

        if (lancamento.getStatus() != Lancamento.StatusLancamento.PENDENTE) {
            throw new IllegalStateException(
                    "Somente lançamentos pendentes podem ser pagos"
            );
        }

        if (request == null
                || request.valorPago() == null
                || request.valorPago().signum() <= 0
                || request.dataPagamento() == null) {
            throw new IllegalArgumentException(
                    "Informe um valor positivo e a data do pagamento"
            );
        }

        lancamento.setValorPago(request.valorPago());
        lancamento.setDataPagamento(request.dataPagamento());
        lancamento.setStatus(Lancamento.StatusLancamento.PAGO);

        Lancamento salvo = lancamentoRepository.save(lancamento);

        return LancamentoResponseDTO.fromEntity(salvo);
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

        BigDecimal totalPrevisto = BigDecimal.ZERO;
        BigDecimal totalPago = BigDecimal.ZERO;
        BigDecimal totalPendente = BigDecimal.ZERO;

        long quantidadePendentes = 0;
        long quantidadePagos = 0;

        for (Lancamento lancamento : lancamentos) {
            switch (lancamento.getStatus()) {

                case PENDENTE -> {
                    totalPrevisto = totalPrevisto.add(
                            lancamento.getValorPrevisto()
                    );

                    totalPendente = totalPendente.add(
                            lancamento.getValorPrevisto()
                    );

                    quantidadePendentes++;
                }

                case PAGO -> {
                    totalPrevisto = totalPrevisto.add(
                            lancamento.getValorPrevisto()
                    );

                    totalPago = totalPago.add(
                            lancamento.getValorPago()
                    );

                    quantidadePagos++;
                }

                case CANCELADO -> {
                    // Cancelados não entram nos totais ativos.
                }
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
