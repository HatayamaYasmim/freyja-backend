package br.com.freyja.controller;

import br.com.freyja.dto.CategoriaDTO;
import br.com.freyja.model.Categoria;
import br.com.freyja.service.CategoriaService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/categorias")
@CrossOrigin(origins = "*")
public class CategoriaController {
    private final CategoriaService service;

    public CategoriaController(CategoriaService service) {
        this.service = service;
    }

    @PostMapping
    public Categoria create(@RequestBody @Valid CategoriaDTO dto) {
        Categoria categoria = new Categoria();
        categoria.setNome(dto.getNome());
        return service.salvar(categoria);
    }

    @GetMapping
    public List<Categoria> listAll() {
        return service.listarTodos();
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.deletar(id);
    }
}
