
package br.com.freyja.service;

import br.com.freyja.dto.receita.ReceitaRequestDTO;
import br.com.freyja.dto.receita.ReceitaResponseDTO;
import br.com.freyja.enums.StatusReceita;
import br.com.freyja.exception.ResourceNotFoundException;
import br.com.freyja.model.Grupo;
import br.com.freyja.model.Receita;
import br.com.freyja.repository.GrupoRepository;
import br.com.freyja.repository.ReceitaRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

@Service
public class ReceitaService {

    private final ReceitaRepository receitaRepository;
    private final GrupoRepository grupoRepository;

    public ReceitaService(
            ReceitaRepository receitaRepository,
            GrupoRepository grupoRepository
    ) {
        this.receitaRepository = receitaRepository;
        this.grupoRepository = grupoRepository;
    }

    @Transactional
    public ReceitaResponseDTO cadastrar(
            ReceitaRequestDTO request
    ) {
        // Competencia representa o mes financeiro.
        if (request.competencia().getDayOfMonth() != 1) {
            throw new IllegalArgumentException(
                    "A competencia deve ser o primeiro dia do mes"
            );
        }

        Grupo grupo = grupoRepository
                .findById(request.grupoId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Grupo não encontrado: " + request.grupoId()
                ));

        Receita receita = new Receita();

        receita.setGrupo(grupo);
        receita.setDescricao(request.descricao().trim());
        receita.setValorPrevisto(request.valorPrevisto());
        receita.setValorRecebido(BigDecimal.ZERO);

        receita.setCompetencia(request.competencia());
        receita.setDataPrevista(request.dataPrevista());

        receita.setDataRecebimento(null);
        receita.setStatus(StatusReceita.PENDENTE);

        Receita salva = receitaRepository.saveAndFlush(receita);

        return ReceitaResponseDTO.fromEntity(salva);
    }

    @Transactional(readOnly = true)
    public ReceitaResponseDTO buscarPorId(Long id) {
        Receita receita = receitaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Receita não encontrada: " + id
                ));

        return ReceitaResponseDTO.fromEntity(receita);
    }

    @Transactional(readOnly = true)
    public List<ReceitaResponseDTO> listarPorGrupoEMes(
            Long grupoId,
            YearMonth mes
    ) {
        if (!grupoRepository.existsById(grupoId)) {
            throw new ResourceNotFoundException(
                    "Grupo não encontrado: " + grupoId
            );
        }

        return receitaRepository
                .findByGrupo_IdAndCompetenciaGreaterThanEqualAndCompetenciaLessThanOrderByDataPrevistaAscIdAsc(
                        grupoId,
                        mes.atDay(1),
                        mes.plusMonths(1).atDay(1)
                )
                .stream()
                .map(ReceitaResponseDTO::fromEntity)
                .toList();
    }
}
