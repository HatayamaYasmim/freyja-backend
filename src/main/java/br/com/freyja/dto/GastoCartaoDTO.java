package br.com.freyja.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class GastoCartaoDTO {
    @NotBlank(message = "Usuário é obrigatório")
    private String usuario;

    @NotNull(message = "Valor é obrigatório")
    @DecimalMin(value = "0.01", message = "Valor deve ser maior que zero")
    private BigDecimal valorTotal;

    @NotNull(message = "Quantidade de parcelas é obrigatória")
    @Min(value = 1, message = "Mínimo 1 parcela")
    private Integer quantidadeParcelas;

    @Size(max = 255, message = "Descrição muito longa")
    private String descricao;

    @Pattern(regexp = "^[APC]$", message = "Status deve ser A, P ou C")
    private String status; // se null → service define como "A"
}
