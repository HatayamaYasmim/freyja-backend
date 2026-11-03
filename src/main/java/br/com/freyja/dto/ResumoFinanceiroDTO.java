
package br.com.freyja.dto;

import java.math.BigDecimal;
import java.time.YearMonth;

public record ResumoFinanceiroDTO(
        Long grupoId,
        YearMonth competencia,
        BigDecimal totalPrevisto,
        BigDecimal totalPago,
        BigDecimal totalPendente,
        long quantidadePendentes,
        long quantidadePagos
) {
}
