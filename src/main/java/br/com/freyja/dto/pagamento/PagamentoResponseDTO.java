
package br.com.freyja.dto.pagamento;

import br.com.freyja.model.Pagamento;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record PagamentoResponseDTO(
        Long id,
        Long lancamentoId,
        BigDecimal valor,
        LocalDate dataPagamento,
        boolean estornado,
        Instant estornadoEm,
        String motivoEstorno
) {

    public static PagamentoResponseDTO fromEntity(
            Pagamento pagamento
    ) {
        return new PagamentoResponseDTO(
                pagamento.getId(),
                pagamento.getLancamento().getId(),
                pagamento.getValor(),
                pagamento.getDataPagamento(),
                pagamento.isEstornado(),
                pagamento.getEstornadoEm(),
                pagamento.getMotivoEstorno()
        );
    }
}
