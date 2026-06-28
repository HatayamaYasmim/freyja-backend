package br.com.freyja.repository;

import br.com.freyja.model.GastoCartao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;

public interface GastosCartaoRepository extends JpaRepository<GastoCartao, Long> {
    boolean existsByIdExterno(String idExterno);

    @Query("""
    SELECT g FROM GastoCartao g
    WHERE g.cartao.id = :cartaoId
      AND MONTH(g.dataCompra) = :mes
      AND YEAR(g.dataCompra) = :ano
      AND (:categoriaId IS NULL OR g.categoria.id = :categoriaId)
    ORDER BY g.dataCompra DESC
""")
    List<GastoCartao> buscarPorFatura(
            Long cartaoId,
            Integer mes,
            Integer ano,
            Long categoriaId
    );

    @Query("""
        SELECT COALESCE(SUM(g.valor), 0)
        FROM GastoCartao g
        WHERE g.cartao.id = :cartaoId
          AND MONTH(g.dataCompra) = :mes
          AND YEAR(g.dataCompra) = :ano
    """)
    BigDecimal somarTotalPorMes(
            Long cartaoId,
            Integer mes,
            Integer ano
    );

}
