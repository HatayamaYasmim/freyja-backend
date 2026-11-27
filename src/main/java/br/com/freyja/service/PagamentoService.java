
package br.com.freyja.service;

import br.com.freyja.dto.lancamento.LancamentoResponseDTO;
import br.com.freyja.dto.lancamento.PagamentoRequestDTO;
import br.com.freyja.dto.pagamento.EstornarPagamentoRequestDTO;
import br.com.freyja.dto.pagamento.PagamentoResponseDTO;
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
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class PagamentoService {

    private final LancamentoRepository lancamentoRepository;
    private final PagamentoRepository pagamentoRepository;

    public PagamentoService(
            LancamentoRepository lancamentoRepository,
            PagamentoRepository pagamentoRepository
    ) {
        this.lancamentoRepository = lancamentoRepository;
        this.pagamentoRepository = pagamentoRepository;
    }

    // Compatibilidade com endpoint antigo: quitação integral.
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

        if (request.valorPago()
                .compareTo(lancamento.getValorPrevisto()) != 0) {
            throw new BusinessConflictException(
                    "Para quitação integral, o valor deve ser igual ao valor previsto"
            );
        }

        return efetuarPagamento(
                lancamento,
                request.valorPago(),
                request.dataPagamento(),
                null
        );
    }

    // Registro de pagamento parcial com idempotência.
    @Transactional
    public LancamentoResponseDTO registrarPagamentoParcial(
            Long lancamentoId,
            Long versaoEsperada,
            RegistrarPagamentoRequestDTO request,
            String idempotencyKey
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

        String chave = validarChaveIdempotencia(idempotencyKey);

        if (chave != null) {
            Optional<Pagamento> existente =
                    pagamentoRepository.findByIdempotencyKey(chave);

            if (existente.isPresent()) {
                Pagamento pagamento = existente.get();

                boolean mesmosDados =
                        Objects.equals(
                                pagamento.getLancamento().getId(),
                                lancamentoId
                        )
                                && pagamento.getValor()
                                .compareTo(request.valor()) == 0
                                && Objects.equals(
                                pagamento.getDataPagamento(),
                                request.dataPagamento()
                        );

                if (!mesmosDados) {
                    throw new BusinessConflictException(
                            "Chave de idempotência já utilizada com dados diferentes"
                    );
                }

                // Não aceitar retry de um pagamento estornado.
                if (pagamento.isEstornado()) {
                    throw new BusinessConflictException(
                            "A chave de idempotência pertence a um pagamento estornado"
                    );
                }

                // Retry confirmado de pagamento ainda ativo.
                // Nenhuma nova gravação é realizada.
                Lancamento lancamento = buscarLancamento(lancamentoId);

                return LancamentoResponseDTO.fromEntity(lancamento);
            }
        }

        Lancamento lancamento = buscarLancamento(lancamentoId);
        validarVersao(lancamento, versaoEsperada);

        return efetuarPagamento(
                lancamento,
                request.valor(),
                request.dataPagamento(),
                chave
        );
    }

    // Sobrecarga para pagamentos sem chave de idempotência.
    @Transactional
    public LancamentoResponseDTO registrarPagamentoParcial(
            Long lancamentoId,
            Long versaoEsperada,
            RegistrarPagamentoRequestDTO request
    ) {
        return registrarPagamentoParcial(
                lancamentoId,
                versaoEsperada,
                request,
                null
        );
    }

    // Recuperação de retries após conflitos de persistência.
    @Transactional(readOnly = true)
    public Optional<LancamentoResponseDTO> recuperarRetryConfirmado(
            Long lancamentoId,
            RegistrarPagamentoRequestDTO request,
            String idempotencyKey
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

        String chave = validarChaveIdempotencia(idempotencyKey);

        if (chave == null) {
            return Optional.empty();
        }

        Optional<Pagamento> existente =
                pagamentoRepository.findByIdempotencyKey(chave);

        if (existente.isEmpty()) {
            return Optional.empty();
        }

        Pagamento pagamento = existente.get();

        boolean mesmosDados =
                Objects.equals(
                        pagamento.getLancamento().getId(),
                        lancamentoId
                )
                        && pagamento.getValor()
                        .compareTo(request.valor()) == 0
                        && Objects.equals(
                        pagamento.getDataPagamento(),
                        request.dataPagamento()
                );

        if (!mesmosDados) {
            throw new BusinessConflictException(
                    "Chave de idempotência já utilizada com dados diferentes"
            );
        }

        // A chave permanece reservada após o estorno.
        if (pagamento.isEstornado()) {
            throw new BusinessConflictException(
                    "O pagamento original foi estornado; utilize uma nova operação"
            );
        }

        Lancamento lancamento = buscarLancamento(lancamentoId);

        return Optional.of(
                LancamentoResponseDTO.fromEntity(lancamento)
        );
    }

    // Histórico completo, incluindo pagamentos estornados.
    @Transactional(readOnly = true)
    public List<PagamentoResponseDTO> listarPagamentos(
            Long lancamentoId
    ) {
        if (lancamentoId == null || lancamentoId <= 0) {
            throw new IllegalArgumentException(
                    "O ID do lançamento deve ser positivo"
            );
        }

        buscarLancamento(lancamentoId);

        return pagamentoRepository
                .findByLancamento_IdOrderByDataPagamentoAscIdAsc(
                        lancamentoId
                )
                .stream()
                .map(PagamentoResponseDTO::fromEntity)
                .toList();
    }

    // Estorno com preservação do histórico.
    @Transactional
    public LancamentoResponseDTO estornarPagamento(
            Long lancamentoId,
            Long pagamentoId,
            Long versaoEsperada,
            EstornarPagamentoRequestDTO request
    ) {
        if (request == null
                || request.motivo() == null
                || request.motivo().isBlank()
                || request.motivo().length() > 500) {
            throw new IllegalArgumentException(
                    "Informe um motivo válido para o estorno"
            );
        }

        Lancamento lancamento = buscarLancamento(lancamentoId);
        validarVersao(lancamento, versaoEsperada);

        Pagamento pagamento = pagamentoRepository
                .findById(pagamentoId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Pagamento não encontrado: " + pagamentoId
                ));

        if (!Objects.equals(
                pagamento.getLancamento().getId(),
                lancamentoId
        )) {
            throw new BusinessConflictException(
                    "O pagamento não pertence ao lançamento informado"
            );
        }

        if (pagamento.isEstornado()) {
            throw new BusinessConflictException(
                    "Este pagamento já foi estornado"
            );
        }

        pagamento.setEstornado(true);
        pagamento.setEstornadoEm(Instant.now());
        pagamento.setMotivoEstorno(request.motivo().trim());

        pagamentoRepository.save(pagamento);

        // Consultar todos os valores antes de alterar Lancamento.
        // Isso evita flush intermediário com incremento adicional
        // da versão do lançamento.
        BigDecimal totalPago =
                pagamentoRepository.somarPagamentosPorLancamento(
                        lancamentoId
                );

        LocalDate ultimaData = null;

        if (totalPago.signum() > 0) {
            ultimaData = pagamentoRepository
                    .findFirstByLancamento_IdAndEstornadoFalseOrderByDataPagamentoDescIdDesc(
                            lancamentoId
                    )
                    .map(Pagamento::getDataPagamento)
                    .orElse(null);
        }

        lancamento.setValorPago(
                totalPago.signum() == 0 ? null : totalPago
        );

        lancamento.setDataPagamento(ultimaData);

        if (totalPago.signum() == 0) {
            lancamento.setStatus(
                    Lancamento.StatusLancamento.PENDENTE
            );
        } else if (totalPago.compareTo(
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

    // Persistência compartilhada entre pagamentos parciais e integrais.
    private LancamentoResponseDTO efetuarPagamento(
            Lancamento lancamento,
            BigDecimal valor,
            LocalDate dataPagamento,
            String idempotencyKey
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

        BigDecimal saldo =
                lancamento.getValorPrevisto().subtract(totalAnterior);

        if (valor.compareTo(saldo) > 0) {
            throw new BusinessConflictException(
                    "O pagamento ultrapassa o saldo pendente"
            );
        }

        Pagamento pagamento = new Pagamento();
        pagamento.setLancamento(lancamento);
        pagamento.setValor(valor);
        pagamento.setDataPagamento(dataPagamento);
        pagamento.setIdempotencyKey(idempotencyKey);

        pagamentoRepository.save(pagamento);

        BigDecimal totalAtual = totalAnterior.add(valor);

        lancamento.setValorPago(totalAtual);
        lancamento.setDataPagamento(dataPagamento);

        if (totalAtual.compareTo(lancamento.getValorPrevisto()) == 0) {
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
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Lançamento não encontrado: " + id
                ));
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

    private String validarChaveIdempotencia(String chave) {
        if (chave == null) {
            return null;
        }

        if (!chave.matches(
                "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-"
                        + "[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-"
                        + "[0-9a-fA-F]{12}$"
        )) {
            throw new IllegalArgumentException(
                    "Idempotency-Key deve ser um UUID válido"
            );
        }

        return UUID.fromString(chave).toString();
    }
}
