
package br.com.freyja.repository;

import br.com.freyja.model.Receita;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ReceitaRepository
        extends JpaRepository<Receita, Long> {

    List<Receita>
    findByGrupo_IdAndCompetenciaGreaterThanEqualAndCompetenciaLessThanOrderByDataPrevistaAscIdAsc(
            Long grupoId,
            LocalDate inicio,
            LocalDate fim
    );
}
