package br.com.freyja.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ContaFixaRequestDTO {
            @NotNull
            Long grupoId;
            @NotNull Long categoriaId;
            @NotBlank
            String tipo;
            @NotNull @DecimalMin("0.01")
            BigDecimal valor;
            @NotBlank String usuarioCriacao;
            @NotNull
            LocalDate dataConta;
            boolean gastoRecorrente;
            String descricao;
}
