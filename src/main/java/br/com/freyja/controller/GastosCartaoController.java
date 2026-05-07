package br.com.freyja.controller;

import br.com.freyja.dto.GastoCartaoDTO;
import br.com.freyja.dto.GastoCartaoPreviewDTO;
import br.com.freyja.model.GastoCartao;
import br.com.freyja.service.GastoCartaoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/gastos")
@CrossOrigin(origins = "*")
public class GastosCartaoController {
    private final GastoCartaoService service;
    public GastosCartaoController(GastoCartaoService service) {
        this.service = service;
    }

    @PostMapping("/{cartaoId}")
    public ResponseEntity<List<GastoCartao>> criar(
            @PathVariable Long cartaoId,
            @RequestBody @Valid GastoCartaoDTO req
    ) {
        return ResponseEntity.ok(service.parcelar(cartaoId, req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<GastoCartao> atualizar(
            @PathVariable Long id,
            @RequestBody GastoCartaoDTO dto
    ) {
        return ResponseEntity.ok(service.atualizar(id, dto));
    }
}
