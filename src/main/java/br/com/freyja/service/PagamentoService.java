
package br.com.freyja.service;

import br.com.freyja.dto.lancamento.LancamentoResponseDTO;
import br.com.freyja.dto.lancamento.PagamentoRequestDTO;
import br.com.freyja.dto.pagamento.RegistrarPagamentoRequestDTO;
import br.com.freyja.exception.BusinessConflictException;
import br.com.freyja.exception.ResourceNotFoundException;
import br.com.freyja.model.Lancamento;
import br.com.freyja.model.Pagamento;
import br.com.freyja.repository.LancamentoRepository;
import br.com.freyja.repository.PagamentoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

@Service
public class PagamentoService {
    //Registro de pagamentos integrais e parciais, total acumulado e saldo.

    private final LancamentoRepository lancamentoRepository;
    private final PagamentoRepository pagamentoRepository;

    public PagamentoService(
            LancamentoRepository lancamentoRepository,
            PagamentoRepository pagamentoRepository
    ) {
        this.lancamentoRepository = lancamentoRepository;
        this.pagamentoRepository = pagamentoRepository;
    }

    // Compatibilidade com o endpoint antigo: quitação integral
    @Transactional
    public LancamentoResponseDTO registrarPagamento(
            Long lancamentoId,
            Long versaoEsperada,
            PagamentoRequestDTO request
    ) {
        if (request == null) {
            throw new IllegalArgumentException(
                    "Os dados do pagamento são obrigatórios"
            );
        }

        validarDadosPagamento(
                request.valorPago(),
                request.dataPagamento()
        );

        Lancamento lancamento = buscarLancamento(lancamentoId);
        validarVersao(lancamento, versaoEsperada);

        if (lancamento.getStatus()
                != Lancamento.StatusLancamento.PENDENTE) {
            throw new BusinessConflictException(
                    "Somente lançamentos pendentes podem ser quitados por este endpoint"
            );
        }

        if (request.valorPago().compareTo(
                lancamento.getValorPrevisto()) != 0) {
            throw new BusinessConflictException(
                    "Para quitação integral, o valor deve ser igual ao valor previsto"
            );
        }

        return efetuarPagamento(
                lancamento,
                request.valorPago(),
                request.dataPagamento()
        );
    }

    @Transactional
    public LancamentoResponseDTO registrarPagamentoParcial(
            Long lancamentoId,
            Long versaoEsperada,
            RegistrarPagamentoRequestDTO request
    ) {
        if (request == null) {
            throw new IllegalArgumentException(
                    "Os dados do pagamento são obrigatórios"
            );
        }

        validarDadosPagamento(
                request.valor(),
                request.dataPagamento()
        );

        Lancamento lancamento = buscarLancamento(lancamentoId);
        validarVersao(lancamento, versaoEsperada);

        return efetuarPagamento(
                lancamento,
                request.valor(),
                request.dataPagamento()
        );
    }

    private LancamentoResponseDTO efetuarPagamento(
            Lancamento lancamento,
            BigDecimal valor,
            LocalDate dataPagamento
    ) {
        if (lancamento.getStatus()
                != Lancamento.StatusLancamento.PENDENTE
                && lancamento.getStatus()
                != Lancamento.StatusLancamento.PARCIAL) {
            throw new BusinessConflictException(
                    "O lançamento não permite novos pagamentos"
            );
        }

        BigDecimal totalAnterior =
                pagamentoRepository.somarPagamentosPorLancamento(
                        lancamento.getId()
                );

        BigDecimal saldo = lancamento.getValorPrevisto()
                .subtract(totalAnterior);

        if (valor.compareTo(saldo) > 0) {
            throw new BusinessConflictException(
                    "O pagamento ultrapassa o saldo pendente"
            );
        }

        Pagamento pagamento = new Pagamento();
        pagamento.setLancamento(lancamento);
        pagamento.setValor(valor);
        pagamento.setDataPagamento(dataPagamento);

        pagamentoRepository.save(pagamento);

        BigDecimal totalAtual = totalAnterior.add(valor);

        lancamento.setValorPago(totalAtual);
        lancamento.setDataPagamento(dataPagamento);

        if (totalAtual.compareTo(
                lancamento.getValorPrevisto()) == 0) {
            lancamento.setStatus(
                    Lancamento.StatusLancamento.PAGO
            );
        } else {
            lancamento.setStatus(
                    Lancamento.StatusLancamento.PARCIAL
            );
        }

        Lancamento salvo =
                lancamentoRepository.saveAndFlush(lancamento);

        return LancamentoResponseDTO.fromEntity(salvo);
    }

    private Lancamento buscarLancamento(Long id) {
        return lancamentoRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Lançamento não encontrado: " + id
                        )
                );
    }

    private void validarDadosPagamento(
            BigDecimal valor,
            LocalDate dataPagamento
    ) {
        if (valor == null
                || valor.signum() <= 0
                || valor.scale() > 2
                || valor.precision() - valor.scale() > 13
                || dataPagamento == null) {
            throw new IllegalArgumentException(
                    "Informe um valor positivo, com até duas casas decimais, e a data do pagamento"
            );
        }
    }

    private void validarVersao(
            Lancamento lancamento,
            Long versaoEsperada
    ) {
        if (versaoEsperada == null || versaoEsperada < 0) {
            throw new IllegalArgumentException(
                    "A versão informada é inválida"
            );
        }

        if (!Objects.equals(
                lancamento.getVersion(),
                versaoEsperada
        )) {
            throw new BusinessConflictException(
                    "Este lançamento foi alterado. Atualize os dados e tente novamente."
            );
        }
    }
}
