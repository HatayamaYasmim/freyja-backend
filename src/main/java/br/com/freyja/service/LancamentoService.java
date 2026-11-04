
package br.com.freyja.service;

import br.com.freyja.dto.*;
import br.com.freyja.exception.BusinessConflictException;
import br.com.freyja.exception.ResourceNotFoundException;
import br.com.freyja.model.Categoria;
import br.com.freyja.model.Grupo;
import br.com.freyja.model.Lancamento;
import br.com.freyja.repository.CategoriaRepository;
import br.com.freyja.repository.GrupoRepository;
import br.com.freyja.repository.LancamentoRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class LancamentoService {

    private final LancamentoRepository lancamentoRepository;
    private final GrupoRepository grupoRepository;
    private final CategoriaRepository categoriaRepository;

    public LancamentoService(
            LancamentoRepository lancamentoRepository,
            GrupoRepository grupoRepository,
            CategoriaRepository categoriaRepository
    ) {
        this.lancamentoRepository = lancamentoRepository;
        this.grupoRepository = grupoRepository;
        this.categoriaRepository = categoriaRepository;
    }

    @Transactional(readOnly = true)
    public List<LancamentoResponseDTO> listarPorGrupo(
            Long grupoId,
            YearMonth mes,
            Long categoriaId,
            Lancamento.StatusLancamento status
    ) {
        validarFiltro(grupoId, mes);

        if (categoriaId != null && categoriaId <= 0) {
            throw new IllegalArgumentException(
                    "O ID da categoria deve ser positivo"
            );
        }


        return lancamentoRepository
                .findByGrupo_IdAndCompetenciaBetween(
                        grupoId,
                        mes.atDay(1),
                        mes.atEndOfMonth()
                )
                .stream()
                .filter(l -> categoriaId == null
                        || l.getCategoria().getId().equals(categoriaId))
                .filter(l -> status == null
                        || l.getStatus() == status)
                .map(LancamentoResponseDTO::fromEntity)
                .toList();
    }


    @Transactional
    public LancamentoResponseDTO registrarPagamento(
            Long lancamentoId,
            Long versaoEsperada,
            PagamentoRequestDTO request
    ) {
        Lancamento lancamento = lancamentoRepository.findById(lancamentoId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Lançamento não encontrado: " + lancamentoId
                ));

        validarVersao(lancamento, versaoEsperada);

        if (lancamento.getStatus() != Lancamento.StatusLancamento.PENDENTE) {
            throw new BusinessConflictException(
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

        Lancamento salvo = lancamentoRepository.saveAndFlush(lancamento);

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

        return calcularResumo(grupoId, mes, lancamentos);
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


    @Transactional
    public LancamentoResponseDTO cadastrarAvulso(
            LancamentoAvulsoRequestDTO request
    ) {
        if (request == null) {
            throw new IllegalArgumentException(
                    "Os dados do lançamento são obrigatórios"
            );
        }

        if (request.competencia() == null
                || request.competencia().getDayOfMonth() != 1) {
            throw new IllegalArgumentException(
                    "A competência deve ser o primeiro dia do mês"
            );
        }

        if (request.descricao() == null
                || request.descricao().isBlank()
                || request.valorPrevisto() == null
                || request.valorPrevisto().signum() <= 0
                || request.vencimento() == null) {
            throw new IllegalArgumentException(
                    "Dados do lançamento inválidos"
            );
        }

        Grupo grupo = grupoRepository.findById(request.grupoId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Grupo não encontrado: " + request.grupoId()
                ));

        Categoria categoria = categoriaRepository
                .findById(request.categoriaId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Categoria não encontrada: " + request.categoriaId()
                ));

        Lancamento lancamento = new Lancamento();

        lancamento.setContaRecorrente(null);
        lancamento.setGrupo(grupo);
        lancamento.setCategoria(categoria);
        lancamento.setDescricao(request.descricao().trim());
        lancamento.setCompetencia(request.competencia());
        lancamento.setVencimento(request.vencimento());
        lancamento.setValorPrevisto(request.valorPrevisto());
        lancamento.setObservacoes(request.observacoes());

        lancamento.setStatus(
                Lancamento.StatusLancamento.PENDENTE
        );

        Lancamento salvo = lancamentoRepository.save(lancamento);

        return LancamentoResponseDTO.fromEntity(salvo);
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
                    // Não entra nos totais ativos
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

    @Transactional
    public LancamentoResponseDTO atualizar(
            Long id,
            Long versaoEsperada,
            AtualizarLancamentoRequestDTO request
    ) {
        if (request == null) {
            throw new IllegalArgumentException(
                    "Os dados da atualização são obrigatórios"
            );
        }

        Lancamento lancamento = lancamentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Lançamento não encontrado: " + id
                ));

        validarVersao(lancamento, versaoEsperada);

        if (lancamento.getStatus() != Lancamento.StatusLancamento.PENDENTE) {
            throw new BusinessConflictException(
                    "Somente lançamentos pendentes podem ser editados"
            );
        }

        if (request.descricao() != null) {
            if (request.descricao().isBlank()) {
                throw new IllegalArgumentException(
                        "A descrição não pode ser vazia"
                );
            }

            lancamento.setDescricao(request.descricao().trim());
        }

        if (request.categoriaId() != null) {
            if (request.categoriaId() <= 0) {
                throw new IllegalArgumentException(
                        "O ID da categoria deve ser positivo"
                );
            }

            Categoria categoria = categoriaRepository
                    .findById(request.categoriaId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Categoria não encontrada: " + request.categoriaId()
                    ));

            lancamento.setCategoria(categoria);
        }

        if (request.valorPrevisto() != null) {
            if (request.valorPrevisto().signum() <= 0) {
                throw new IllegalArgumentException(
                        "O valor previsto deve ser positivo"
                );
            }

            lancamento.setValorPrevisto(request.valorPrevisto());
        }

        if (request.vencimento() != null) {
            lancamento.setVencimento(request.vencimento());
        }

        if (request.observacoes() != null) {
            lancamento.setObservacoes(request.observacoes());
        }

        Lancamento salvo = lancamentoRepository.saveAndFlush(lancamento);

        return LancamentoResponseDTO.fromEntity(salvo);
    }


    @Transactional
    public LancamentoResponseDTO cancelar(Long id, Long versaoEsperada) {
        Lancamento lancamento = lancamentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Lançamento não encontrado: " + id
                ));

        validarVersao(lancamento, versaoEsperada);

        if (lancamento.getStatus() != Lancamento.StatusLancamento.PENDENTE) {
            throw new BusinessConflictException(
                    "Somente lançamentos pendentes podem ser cancelados"
            );
        }

        lancamento.setStatus(Lancamento.StatusLancamento.CANCELADO);

        Lancamento salvo = lancamentoRepository.saveAndFlush(lancamento);

        return LancamentoResponseDTO.fromEntity(salvo);
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

        if (!Objects.equals(lancamento.getVersion(), versaoEsperada)) {
            throw new BusinessConflictException(
                    "Este lançamento foi alterado. Atualize os dados e tente novamente."
            );
        }
    }




}
