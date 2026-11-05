package br.com.freyja.dto.pagamento;

import br.com.freyja.model.Pagamento;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PagamentoResponseDTO(
        Long id,
        Long lancamentoId,
        BigDecimal valor,
        LocalDate dataPagamento
) {
    public static PagamentoResponseDTO fromEntiy(Pagamento pagamento) {
        return new PagamentoResponseDTO(
                pagamento.getId(),
                pagamento.getLancamento().getId(),
                pagamento.getValor(),
                pagamento.getDataPagamento()
        );
    }
}
