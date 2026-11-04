
package br.com.freyja.dto.contarecorrente;

import br.com.freyja.model.ContaRecorrente;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ContaRecorrenteResponseDTO(
        Long id,
        String descricao,
        Long grupoId,
        String grupoNome,
        Long categoriaId,
        String categoriaNome,
        BigDecimal valorPrevisto,
        String periodicidade,
        Integer diaVencimento,
        LocalDate dataInicio,
        LocalDate dataFim,
        Boolean ativa
) {

    public static ContaRecorrenteResponseDTO fromEntity(
            ContaRecorrente conta
    ) {
        return new ContaRecorrenteResponseDTO(
                conta.getId(),
                conta.getDescricao(),
                conta.getGrupo().getId(),
                conta.getGrupo().getNome(),
                conta.getCategoria().getId(),
                conta.getCategoria().getNome(),
                conta.getValorPrevisto(),
                conta.getPeriodicidade().name(),
                conta.getDiaVencimento(),
                conta.getDataInicio(),
                conta.getDataFim(),
                conta.isAtiva());
    }
}
