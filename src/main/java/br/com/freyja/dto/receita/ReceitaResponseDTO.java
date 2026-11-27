
package br.com.freyja.dto.receita;

import br.com.freyja.model.Receita;
import br.com.freyja.enums.StatusReceita;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ReceitaResponseDTO(
        Long id,
        Long grupoId,
        String descricao,
        BigDecimal valorPrevisto,
        BigDecimal valorRecebido,
        LocalDate competencia,
        LocalDate dataPrevista,
        LocalDate dataRecebimento,
        StatusReceita status,
        Long version
) {

    public static ReceitaResponseDTO fromEntity(
            Receita receita
    ) {
        return new ReceitaResponseDTO(
                receita.getId(),
                receita.getGrupo().getId(),
                receita.getDescricao(),
                receita.getValorPrevisto(),
                receita.getValorRecebido(),
                receita.getCompetencia(),
                receita.getDataPrevista(),
                receita.getDataRecebimento(),
                receita.getStatus(),
                receita.getVersion()
        );
    }
}
