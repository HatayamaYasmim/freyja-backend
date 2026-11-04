package br.com.freyja.service;

import br.com.freyja.dto.contarecorrente.AtualizarContaRecorrenteRequestDTO;
import br.com.freyja.dto.contarecorrente.ContaRecorrenteRequestDTO;
import br.com.freyja.dto.contarecorrente.ContaRecorrenteResponseDTO;
import br.com.freyja.dto.contarecorrente.EncerrarContaRecorrenteRequestDTO;
import br.com.freyja.exception.BusinessConflictException;
import br.com.freyja.exception.ResourceNotFoundException;
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

        return ContaRecorrenteResponseDTO.fromEntity(salva);
    }

    @Transactional(readOnly = true)
    public List<ContaRecorrenteResponseDTO> listarPorGrupo(Long grupoId) {
        return repository.findByGrupo_Id(grupoId)
                .stream()
                .map(ContaRecorrenteResponseDTO::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public ContaRecorrenteResponseDTO findByGrupo_Id(Long id) {
        ContaRecorrente conta = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Conta recorrente não encontrada: " + id
                ));

        return ContaRecorrenteResponseDTO.fromEntity(conta);
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


    @Transactional
    public ContaRecorrenteResponseDTO atualizar(
            Long id,
            AtualizarContaRecorrenteRequestDTO request
    ) {
        if (request == null) {
            throw new IllegalArgumentException(
                    "Os dados da atualização são obrigatórios"
            );
        }

        ContaRecorrente conta = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Conta recorrente não encontrada: " + id
                ));

        if (request.descricao() != null) {
            if (request.descricao().isBlank()) {
                throw new IllegalArgumentException(
                        "A descrição não pode ser vazia"
                );
            }

            conta.setDescricao(request.descricao().trim());
        }

        if (request.categoriaId() != null) {
            Categoria categoria = categoriaRepository
                    .findById(request.categoriaId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Categoria não encontrada: " + request.categoriaId()
                    ));

            conta.setCategoria(categoria);
        }

        if (request.valorPrevisto() != null) {
            if (request.valorPrevisto().signum() <= 0) {
                throw new IllegalArgumentException(
                        "O valor previsto deve ser positivo"
                );
            }

            conta.setValorPrevisto(request.valorPrevisto());
        }

        if (request.diaVencimento() != null) {
            if (request.diaVencimento() < 1
                    || request.diaVencimento() > 31) {
                throw new IllegalArgumentException(
                        "O dia de vencimento deve estar entre 1 e 31"
                );
            }

            conta.setDiaVencimento(request.diaVencimento());
        }

        ContaRecorrente salva = repository.save(conta);

        return ContaRecorrenteResponseDTO.fromEntity(salva);
    }


    @Transactional
    public ContaRecorrenteResponseDTO desativar(Long id) {
        ContaRecorrente conta = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Conta recorrente não encontrada: " + id
                ));

        if (!conta.isAtiva()) {
            throw new BusinessConflictException(
                    "Esta conta recorrente já está desativada"
            );
        }

        conta.setAtiva(false);

        ContaRecorrente salva = repository.save(conta);

        return ContaRecorrenteResponseDTO.fromEntity(salva);
    }


    @Transactional
    public ContaRecorrenteResponseDTO encerrar(
            Long id,
            EncerrarContaRecorrenteRequestDTO request
    ) {
        if (request == null || request.dataFim() == null) {
            throw new IllegalArgumentException(
                    "A data de encerramento é obrigatória"
            );
        }

        ContaRecorrente conta = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Conta recorrente não encontrada: " + id
                ));

        if (!conta.isAtiva()) {
            throw new BusinessConflictException(
                    "Não é possível encerrar uma conta desativada"
            );
        }

        if (conta.getDataFim() != null) {
            throw new BusinessConflictException(
                    "Esta conta recorrente já possui uma data de encerramento"
            );
        }

        if (request.dataFim().isBefore(conta.getDataInicio())) {
            throw new IllegalArgumentException(
                    "A data de encerramento não pode ser anterior à data inicial"
            );
        }

        conta.setDataFim(request.dataFim());

        ContaRecorrente salva = repository.save(conta);

        return ContaRecorrenteResponseDTO.fromEntity(salva);
    }



}
