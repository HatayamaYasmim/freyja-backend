package br.com.freyja.controller;

import br.com.freyja.dto.ContaFixaRequestDTO;
import br.com.freyja.model.ContaFixa;
import br.com.freyja.service.ContaFixaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/contas-fixas")
@CrossOrigin(origins = "*")
public class ContaFixaController {
    private  final ContaFixaService service;

    public ContaFixaController(ContaFixaService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ContaFixa> save (@RequestBody @Valid ContaFixaRequestDTO  dto) {
        return  ResponseEntity.ok(service.save(dto));
    }

    @GetMapping("/grupo/{grupoId}")
    public ResponseEntity<List<ContaFixa>> listarPorGrupoMes(
            @PathVariable Long grupoId,
            @RequestParam Integer mes,
            @RequestParam Integer ano
    ) {
        return ResponseEntity.ok(service.listByMesGrupo(grupoId, mes, ano));
    }
}
