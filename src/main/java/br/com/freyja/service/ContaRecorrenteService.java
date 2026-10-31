package br.com.freyja.service;

import br.com.freyja.dto.ContaRecorrenteRequestDTO;
import br.com.freyja.dto.ContaRecorrenteResponseDTO;
import br.com.freyja.model.Categoria;
import br.com.freyja.model.ContaRecorrente;
import br.com.freyja.model.Grupo;
import br.com.freyja.repository.CategoriaRepository;
import br.com.freyja.repository.ContaRecorrenteRepository;
import br.com.freyja.repository.GrupoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class ContaRecorrenteService {
    private final ContaRecorrenteRepository repository;
    private final GrupoRepository grupoRepository;
    private final CategoriaRepository categoriaRepository;

    public ContaRecorrenteService(ContaRecorrenteRepository repository, GrupoRepository grupoRepository, CategoriaRepository categoriaRepository) {
        this.repository = repository;
        this.categoriaRepository = categoriaRepository;
        this.grupoRepository = grupoRepository;
    }

    @Transactional
    public ContaRecorrenteResponseDTO cadastrar(ContaRecorrenteRequestDTO dto) {
        validarDatas(dto);

        Grupo grupo = grupoRepository.findById(dto.grupoId())
                .orElseThrow(() -> new IllegalArgumentException("Grupo não encontrado"));

        Categoria categoria = categoriaRepository.findById(dto.categoriaId())
                .orElseThrow(() -> new IllegalArgumentException("Categoria não encontrada"));

        ContaRecorrente conta = new ContaRecorrente();

        conta.setDescricao(dto.descricao());
        conta.setGrupo(grupo);
        conta.setCategoria(categoria);
        conta.setValorPrevisto(dto.valorPrevisto());
        conta.setPeriodicidade(
                converterPeriodicidade(dto.periodicidade())
        );
        conta.setDiaVencimento(dto.diaVencimento());
        conta.setDataInicio(dto.dataInicio());
        conta.setDataFim(dto.dataFim());
        conta.setAtiva(true);

        ContaRecorrente salva = repository.save(conta);

        return converterResponse(salva);
    }

    @Transactional(readOnly = true)
    public List<ContaRecorrenteResponseDTO> listarPorGrupo(Long grupoId) {
        return repository.findByGrupo_Id(grupoId)
                .stream()
                .map(this::converterResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ContaRecorrenteResponseDTO findByGrupo_Id(Long id) {
        ContaRecorrente conta = repository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Conta recorrente não encontrada"
                        )
                );

        return converterResponse(conta);
    }

    private void validarDatas(ContaRecorrenteRequestDTO dto) {
        if (dto.dataInicio() != null &&
                dto.dataFim() != null &&
                dto.dataFim().isBefore(dto.dataInicio())) {

            throw new IllegalArgumentException(
                    "A data final não pode ser anterior à inicial"
            );
        }
    }

    private ContaRecorrente.Periodicidade converterPeriodicidade(
            String periodicidade
    ) {
        try {
            return ContaRecorrente.Periodicidade.valueOf(
                    periodicidade.trim().toUpperCase(Locale.ROOT)
            );
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException(
                    "Periodicidade inválida. Use MENSAL ou ANUAL"
            );
        }
    }

    private ContaRecorrenteResponseDTO converterResponse(
            ContaRecorrente conta
    ) {
        return new ContaRecorrenteResponseDTO(
                conta.getId(),
                conta.getDescricao(),
                conta.getGrupo().getId(),
                conta.getGrupo().getNome(),
                conta.getCategoria().getId(),
                conta.getCategoria().getNome(),
                conta.getValorPrevisto(),
                conta.getPeriodicidade().name(),
                conta.getDiaVencimento(),
                conta.getDataInicio(),
                conta.getDataFim(),
                null
        );
    }
}
