
package br.com.freyja.dto.filtro;

import br.com.freyja.model.Lancamento.StatusLancamento;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.YearMonth;

public record LancamentoFiltroDTO(
        @DateTimeFormat(pattern = "yyyy-MM")
        YearMonth mes,

        Long categoriaId,

        StatusLancamento status
) {
}
