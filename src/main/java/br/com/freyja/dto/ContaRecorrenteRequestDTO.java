package br.com.freyja.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ContaRecorrenteRequestDTO(
        @NotBlank(message = "A descrição é obrigatória")
        @Size(max = 255)
        String descricao,

        @NotNull(message = "O grupo é obrigatório")
        Long grupoId,

        @NotNull(message = "A categoria é obrigatória")
        Long categoriaId,

        @NotNull(message = "O valor previsto é obrigatório")
        @DecimalMin(value = "0.01")
        @Digits(integer = 13, fraction = 2)
        BigDecimal valorPrevisto,

        @NotBlank(message = "A periodicidade é obrigatória")
        String periodicidade,

        @NotNull(message = "O dia do vencimento é obrigatório")
        @Min(1)
        @Max(31)
        Integer diaVencimento,

        @NotNull(message = "A data de início é obrigatória")
        LocalDate dataInicio,

        LocalDate dataFim,

        String observacao) {
}
