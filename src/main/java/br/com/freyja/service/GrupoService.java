package br.com.freyja.service;

import br.com.freyja.model.Grupo;
import br.com.freyja.repository.GrupoRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class GrupoService {
    private final GrupoRepository grupoRepository;

    public GrupoService(GrupoRepository grupoRepository) {
        this.grupoRepository = grupoRepository;
    }

    public Grupo create(Grupo grupo) {
        return grupoRepository.save(grupo);
    }

    public List<Grupo> listAll() {
        return grupoRepository.findAll();
    }

    public Optional<Grupo> listById(Long id) {
        return grupoRepository.findById(id);
    }

    public void delete(Long id) {
        grupoRepository.deleteById(id);
    }

    public Grupo update(Long id, Grupo grupoAtualizado) {
        return grupoRepository.findById(id)
                .map(grupo -> {
                    grupo.setNome(grupoAtualizado.getNome());
                    return grupoRepository.save(grupo);
                })
                .orElseThrow(() -> new RuntimeException("Grupo não encontrado"));
    }
}
