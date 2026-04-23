package br.com.freyja.controller;

import br.com.freyja.dto.GrupoDTO;
import br.com.freyja.model.Grupo;
import br.com.freyja.service.GrupoService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/grupos")
@CrossOrigin(origins = "*")
public class GrupoController {

    private final GrupoService service;

    public GrupoController(GrupoService service) {
        this.service = service;
    }

    @PostMapping
    public Grupo create(@RequestBody @Valid GrupoDTO dto) {
        Grupo grupo = new Grupo();
        grupo.setNome(dto.getNome());

        return service.create(grupo);
    }

    @GetMapping
    public List<Grupo> listAll() {
        return service.listAll();
    }

    @GetMapping("/{id}")
    public Grupo listById(@PathVariable Long id) {
        return service.listById(id)
                .orElseThrow(() -> new RuntimeException("Grupo não encontrado"));
    }

    @PutMapping("/{id}")
    public Grupo update(@PathVariable Long id, @RequestBody @Valid GrupoDTO dto) {
        Grupo grupo = new Grupo();
        grupo.setNome(dto.getNome());

        return service.update(id, grupo);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}