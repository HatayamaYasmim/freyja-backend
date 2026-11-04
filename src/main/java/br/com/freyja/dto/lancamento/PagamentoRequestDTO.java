
package br.com.freyja.dto.lancamento;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PagamentoRequestDTO(
        @NotNull
        @DecimalMin("0.01")
        BigDecimal valorPago,

        @NotNull
        LocalDate dataPagamento
) {
}
