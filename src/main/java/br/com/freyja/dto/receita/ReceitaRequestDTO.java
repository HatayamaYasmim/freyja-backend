
package br.com.freyja.dto.receita;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ReceitaRequestDTO(

        @NotNull
        @Positive
        Long grupoId,

        @NotBlank
        @Size(max = 255)
        String descricao,

        @NotNull
        @DecimalMin("0.01")
        @Digits(integer = 13, fraction = 2)
        BigDecimal valorPrevisto,

        @NotNull
        LocalDate competencia,

        @NotNull
        LocalDate dataPrevista

) {}
