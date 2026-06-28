package br.com.freyja.controller;

import br.com.freyja.dto.GastoCartaoDTO;
import br.com.freyja.dto.GastoCartaoPreviewDTO;
import br.com.freyja.dto.GastoCartaoResponseDTO;
import br.com.freyja.model.GastoCartao;
import br.com.freyja.service.GastoCartaoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
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
        return ResponseEntity.ok(service.cadastrar(cartaoId, req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<GastoCartao> atualizar(
            @PathVariable Long id,
            @RequestBody GastoCartaoDTO dto
    ) {
        return ResponseEntity.ok(service.atualizar(id, dto));
    }

    @GetMapping("/cartao/{cartaoId}/fatura")
    public ResponseEntity<List<GastoCartaoResponseDTO>> listarFaturaAtual(
            @PathVariable Long cartaoId,
            @RequestParam Integer mes,
            @RequestParam Integer ano,
            @RequestParam(required = false) Long categoriaId
    ) {
        return ResponseEntity.ok(
                service.listarPorFatura(cartaoId, mes, ano, categoriaId)
        );
    }

    @GetMapping("/cartao/{cartaoId}/total-mes")
    public ResponseEntity<BigDecimal> buscarTotalMes(
            @PathVariable Long cartaoId,
            @RequestParam Integer mes,
            @RequestParam Integer ano
    ) {
        return ResponseEntity.ok(
                service.buscarTotalMes(cartaoId, mes, ano)
        );
    }
}
