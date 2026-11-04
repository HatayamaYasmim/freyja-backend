
package br.com.freyja.dto;

import br.com.freyja.model.Lancamento;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LancamentoResponseDTO(
        Long id,
        Long contaRecorrenteId,
        Long grupoId,
        Long categoriaId,
        String descricao,
        LocalDate competencia,
        LocalDate vencimento,
        BigDecimal valorPrevisto,
        BigDecimal valorPago,
        LocalDate dataPagamento,
        String status,
        String observacoes,
        Long version
) {
    public static LancamentoResponseDTO fromEntity(Lancamento lancamento) {
        return new LancamentoResponseDTO(
                lancamento.getId(),
                lancamento.getContaRecorrente() != null
                        ? lancamento.getContaRecorrente().getId()
                        : null,
                lancamento.getGrupo().getId(),
                lancamento.getCategoria().getId(),
                lancamento.getDescricao(),
                lancamento.getCompetencia(),
                lancamento.getVencimento(),
                lancamento.getValorPrevisto(),
                lancamento.getValorPago(),
                lancamento.getDataPagamento(),
                lancamento.getStatus().name(),
                lancamento.getObservacoes(),
                lancamento.getVersion()
        );
    }
}
