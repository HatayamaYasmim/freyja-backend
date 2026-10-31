package br.com.freyja.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ContaRecorrenteResponseDTO(
        Long id,
        String descricao,
        Long grupoId,
        String grupoNome,
        Long categoriaId,
        String categoriaNome,
        BigDecimal valorPrevisto,
        String periodicidade,
        Integer diaVencimento,
        LocalDate dataInicio,
        LocalDate dataFim,
        String observacao
) {}