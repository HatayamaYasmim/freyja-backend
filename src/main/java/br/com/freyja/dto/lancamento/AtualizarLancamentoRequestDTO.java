
package br.com.freyja.dto.lancamento;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AtualizarLancamentoRequestDTO(
        @Size(max = 255)
        String descricao,

        @Positive
        Long categoriaId,

        @DecimalMin("0.01")
        @Digits(integer = 13, fraction = 2)
        BigDecimal valorPrevisto,

        LocalDate vencimento,

        String observacoes
) {
}
