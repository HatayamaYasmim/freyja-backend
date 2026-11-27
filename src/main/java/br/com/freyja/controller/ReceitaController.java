
package br.com.freyja.controller;

import br.com.freyja.dto.receita.ReceitaRequestDTO;
import br.com.freyja.dto.receita.ReceitaResponseDTO;
import br.com.freyja.service.ReceitaService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/receitas")
@Validated
public class ReceitaController {

    private final ReceitaService receitaService;

    public ReceitaController(ReceitaService receitaService) {
        this.receitaService = receitaService;
    }

    @PostMapping
    public ResponseEntity<ReceitaResponseDTO> cadastrar(
            @Valid @RequestBody ReceitaRequestDTO request
    ) {
        ReceitaResponseDTO response =
                receitaService.cadastrar(request);

        return ResponseEntity
                .created(URI.create("/receitas/" + response.id()))
                .body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReceitaResponseDTO> buscarPorId(
            @PathVariable @Positive Long id
    ) {
        return ResponseEntity.ok(
                receitaService.buscarPorId(id)
        );
    }

    @GetMapping("/grupo/{grupoId}")
    public ResponseEntity<List<ReceitaResponseDTO>> listar(
            @PathVariable @Positive Long grupoId,
            @RequestParam("mes")
            @DateTimeFormat(pattern = "yyyy-MM")
            YearMonth mes
    ) {
        return ResponseEntity.ok(
                receitaService.listarPorGrupoEMes(grupoId, mes)
        );
    }
}
