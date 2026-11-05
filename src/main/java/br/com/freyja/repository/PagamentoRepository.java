package br.com.freyja.repository;

import br.com.freyja.model.Pagamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface PagamentoRepository extends JpaRepository<Pagamento, Long> {

    List<Pagamento> findByLancamento_IdOrderByDataPagamentoAscIdAsc(Long lancamentoId);

    @Query("""
            SELECT COALESCE(SUM(p.valor), 0)
            FROM Pagamento p
            WHERE p.lancamento.id = :lancamentoId
            """)
    BigDecimal somarPagamentosPorLancamento(@Param("lancamentoId") Long lancamentoId);
}
