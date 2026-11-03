
package br.com.freyja.controller;

import br.com.freyja.dto.LancamentosRequestDTO;
import br.com.freyja.dto.LancamentoResponseDTO;
import br.com.freyja.dto.PagamentoRequestDTO;
import br.com.freyja.dto.ResumoFinanceiroDTO;
import br.com.freyja.dto.filtro.PeriodoMensalFiltroDTO;
import br.com.freyja.service.GeracaoLancamentosService;
import br.com.freyja.service.LancamentoService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/lancamentos")
@Validated
public class LancamentoController {

    private final GeracaoLancamentosService geracaoService;
    private final LancamentoService lancamentoService;

    public LancamentoController(
            GeracaoLancamentosService geracaoService,
            LancamentoService lancamentoService
    ) {
        this.geracaoService = geracaoService;
        this.lancamentoService = lancamentoService;
    }

    @PostMapping("/gerar")
    public ResponseEntity<List<LancamentoResponseDTO>> gerar(
            @Valid @RequestBody LancamentosRequestDTO request
    ) {
        List<LancamentoResponseDTO> resposta = geracaoService.gerar(
                        request.contaRecorrenteId(),
                        request.inicio(),
                        request.fim()
                )
                .stream()
                .map(LancamentoResponseDTO::fromEntity)
                .toList();

        return ResponseEntity.ok(resposta);
    }

    @GetMapping("/grupo/{grupoId}")
    public ResponseEntity<List<LancamentoResponseDTO>> listarPorGrupo(
            @PathVariable @Positive Long grupoId,
            @RequestParam
            @DateTimeFormat(pattern = "yyyy-MM")
            YearMonth mes
    ) {
        return ResponseEntity.ok(
                lancamentoService.listarPorGrupo(grupoId, mes)
        );
    }


    @PatchMapping("/{id}/pagamento")
    public ResponseEntity<LancamentoResponseDTO> registrarPagamento(
            @PathVariable @Positive Long id,
            @Valid @RequestBody PagamentoRequestDTO request
    ) {
        return ResponseEntity.ok(
                lancamentoService.registrarPagamento(id, request)
        );
    }


    @GetMapping("/grupo/{grupoId}/resumo")
    public ResponseEntity<ResumoFinanceiroDTO> obterResumo(
            @PathVariable @Positive Long grupoId,
            @Valid @ModelAttribute PeriodoMensalFiltroDTO filtro
    ) {
        return ResponseEntity.ok(
                lancamentoService.obterResumo(
                        grupoId,
                        filtro.mes()
                )
        );
    }


}
