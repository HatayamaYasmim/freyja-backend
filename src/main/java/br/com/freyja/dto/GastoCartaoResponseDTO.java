package br.com.freyja.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record GastoCartaoResponseDTO(
        Long id,
        LocalDate dataCompra,
        String descricao,
        BigDecimal valor,
        Integer parcela,
        Integer quantidadeParcelas,
        String status,
        Long categoriaId,
        String categoriaNome
) {}