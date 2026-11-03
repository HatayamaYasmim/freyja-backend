package br.com.freyja.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.YearMonth;

public record LancamentosRequestDTO(@NotNull @Positive Long contaRecorrenteId,
                                    @NotNull YearMonth inicio,
                                    @NotNull YearMonth fim)
{
}
