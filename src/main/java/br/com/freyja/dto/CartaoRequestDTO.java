//package br.com.freyja.dto;
//
//import jakarta.validation.constraints.*;
//import java.math.BigDecimal;
//
//public record CreateCartaoDTO(
//
//        @NotBlank(message = "Nome é obrigatório")
//        String nome,
//
//        @NotNull(message = "Dia de vencimento é obrigatório")
//        @Min(value = 1, message = "Dia inválido")
//        @Max(value = 31, message = "Dia inválido")
//        Integer diaVencimento,
//
//        @NotNull(message = "Limite é obrigatório")
//        @DecimalMin(value = "0.01", message = "Limite deve ser maior que zero")
//        BigDecimal limite,
//
//        @Pattern(regexp = "^[AI]$", message = "Status deve ser A ou I")
//        String status,
//
//        @NotBlank(message = "Proprietário é obrigatório")
//        String proprietario
//
//) {}
package br.com.freyja.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record CartaoRequestDTO(

        @NotBlank(message = "Nome é obrigatório")
        String nome,

        @NotNull(message = "Dia de vencimento é obrigatório")
        @Min(value = 1, message = "Dia inválido")
        @Max(value = 31, message = "Dia inválido")
        Integer diaVencimento,

        @NotNull(message = "Limite é obrigatório")
        @DecimalMin(value = "0.01", message = "Limite deve ser maior que zero")
        BigDecimal limite,

        @Pattern(regexp = "^[AI]$", message = "Status deve ser A ou I")
        String status,

        @NotBlank(message = "Proprietário é obrigatório")
        String proprietario

) {}
