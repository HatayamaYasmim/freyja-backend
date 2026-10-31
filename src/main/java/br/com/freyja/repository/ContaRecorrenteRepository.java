package br.com.freyja.repository;


import br.com.freyja.model.ContaRecorrente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContaRecorrenteRepository extends JpaRepository<ContaRecorrente, Long> {
    List<ContaRecorrente> findByGrupo_Id(Long grupoId);
}
