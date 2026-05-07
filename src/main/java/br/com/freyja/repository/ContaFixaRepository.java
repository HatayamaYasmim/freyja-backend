package br.com.freyja.repository;

import br.com.freyja.model.ContaFixa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ContaFixaRepository extends JpaRepository<ContaFixa, Long> {
    List<ContaFixa> findByGrupoIdAndDataContaBetween(Long grupoId, LocalDate inicio, LocalDate fim);
}
