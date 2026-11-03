package br.com.freyja.repository;

import br.com.freyja.model.Lancamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LancamentoRepository extends JpaRepository<Lancamento, Long> {
    List<Lancamento> findByGrupo_IdAndCompetenciaBetween(
            Long grupoId,
            LocalDate inicio,
            LocalDate fim
    );

    List<Lancamento> findByContaRecorrente_IdOrderByCompetenciaAsc(
            Long contaRecorrenteId
    );

    boolean existsByContaRecorrente_IdAndCompetencia(
            Long contaRecorrenteId,
            LocalDate competencia
    );
}
