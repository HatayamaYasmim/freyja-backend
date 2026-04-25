package br.com.freyja.service;

import br.com.freyja.dto.ContaFixaRequestDTO;
import br.com.freyja.model.Categoria;
import br.com.freyja.model.ContaFixa;
import br.com.freyja.model.Grupo;
import br.com.freyja.repository.CategoriaRepository;
import br.com.freyja.repository.ContaFixaRepository;
import br.com.freyja.repository.GrupoRepository;
import org.springframework.stereotype.Service;

@Service
public class ContaFixaService {

    private final ContaFixaRepository contaFixaRepo;
    private final GrupoRepository grupoRepo;
    private final CategoriaRepository categoriaRepo;

    public ContaFixaService( ContaFixaRepository contaFixaRepo,
                             GrupoRepository grupoRepo,
                             CategoriaRepository categoriaRepo) {
        this.contaFixaRepo = contaFixaRepo;
        this.grupoRepo = grupoRepo;
        this.categoriaRepo = categoriaRepo;
    }

    public ContaFixa save(ContaFixaRequestDTO dto) {
        Grupo grupo = grupoRepo.findById(dto.getGrupoId()).orElseThrow(() -> new RuntimeException("Grupo não encontrado."));
        Categoria categoria = categoriaRepo.findById(dto.getCategoriaId())
                .orElseThrow(() -> new RuntimeException("Categoria não encontrada"));

        ContaFixa conta = new ContaFixa();
        conta.setGrupo(grupo);
        conta.setCategoria(categoria);
        conta.setTipo(dto.getTipo());
        conta.setValor(dto.getValor());
        conta.setUsuarioCriacao(dto.getUsuarioCriacao());
        conta.setDataConta(dto.getDataConta());
        conta.setGastoRecorrente(dto.isGastoRecorrente());
        conta.setDescricao(dto.getDescricao());

        return contaFixaRepo.save(conta);
    }

}
