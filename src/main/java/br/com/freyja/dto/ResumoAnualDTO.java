
package br.com.freyja.dto;

import java.math.BigDecimal;
import java.util.List;

public record ResumoAnualDTO(
        Long grupoId,
        Integer ano,
        BigDecimal totalPrevisto,
        BigDecimal totalPago,
        BigDecimal totalPendente,
        List<ResumoFinanceiroDTO> meses
) {
}
