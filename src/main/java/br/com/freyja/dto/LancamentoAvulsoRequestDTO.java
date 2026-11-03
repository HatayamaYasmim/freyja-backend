
package br.com.freyja.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LancamentoAvulsoRequestDTO(

        @NotBlank
        String descricao,

        @NotNull
        @Positive
        Long grupoId,

        @NotNull
        @Positive
        Long categoriaId,

        @NotNull
        LocalDate competencia,

        @NotNull
        LocalDate vencimento,

        @NotNull
        @DecimalMin("0.01")
        @Digits(integer = 13, fraction = 2)
        BigDecimal valorPrevisto,

        String observacoes
) {
}
