
package br.com.freyja.dto.contarecorrente;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record EncerrarContaRecorrenteRequestDTO(
        @NotNull
        LocalDate dataFim
) {
}
