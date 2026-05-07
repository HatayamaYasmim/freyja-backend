package br.com.freyja.repository;

import br.com.freyja.model.GastoCartao;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GastosCartaoRepository extends JpaRepository<GastoCartao, Long> {
    boolean existsByIdExterno(String idExterno);
}
