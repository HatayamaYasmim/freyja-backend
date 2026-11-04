
package br.com.freyja.dto.contarecorrente;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record AtualizarContaRecorrenteRequestDTO(

        @Size(max = 255)
        String descricao,

        @Positive
        Long categoriaId,

        @DecimalMin("0.01")
        @Digits(integer = 13, fraction = 2)
        BigDecimal valorPrevisto,

        @Min(1)
        @Max(31)
        Integer diaVencimento

) {
}
