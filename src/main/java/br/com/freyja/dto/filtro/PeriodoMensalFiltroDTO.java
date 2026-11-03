
package br.com.freyja.dto.filtro;

import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.YearMonth;

public record PeriodoMensalFiltroDTO(
        @NotNull
        @DateTimeFormat(pattern = "yyyy-MM")
        YearMonth mes
) {
}
