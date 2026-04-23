package br.com.freyja.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CartaoListagemResponseDTO {
    private List<CartaoResponseDTO> cartoes;
    private Long totalCartoes;
    private BigDecimal limiteTotal;

    public CartaoListagemResponseDTO(
            List<CartaoResponseDTO> cartoes,
            Long totalCartoes,
            BigDecimal limiteTotal
    ) {
        this.cartoes = cartoes;
        this.totalCartoes = totalCartoes;
        this.limiteTotal = limiteTotal;
    }

}
