
package br.com.freyja.controller;

import br.com.freyja.dto.*;
import br.com.freyja.dto.filtro.LancamentoFiltroDTO;
import br.com.freyja.dto.filtro.PeriodoMensalFiltroDTO;
import br.com.freyja.service.GeracaoLancamentosService;
import br.com.freyja.service.LancamentoService;

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
            @Valid @ModelAttribute LancamentoFiltroDTO filtro
    ) {
        return ResponseEntity.ok(
                lancamentoService.listarPorGrupo(
                        grupoId,
                        filtro.mes(),
                        filtro.categoriaId(),
                        filtro.status()
                )
        );
    }


    @PatchMapping("/{id}/pagamento")
    public ResponseEntity<LancamentoResponseDTO> registrarPagamento(
            @PathVariable @Positive Long id,
            @RequestHeader("If-Match") String ifMatch,
            @Valid @RequestBody PagamentoRequestDTO request
    ) {
        return respostaComEtag(
                lancamentoService.registrarPagamento(
                        id,
                        extrairVersao(ifMatch),
                        request
                )
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


    @PostMapping
    public ResponseEntity<LancamentoResponseDTO> cadastrarAvulso(
            @Valid @RequestBody LancamentoAvulsoRequestDTO request
    ) {
        LancamentoResponseDTO response =
                lancamentoService.cadastrarAvulso(request);

        URI location = URI.create(
                "/lancamentos/" + response.id()
        );

        return ResponseEntity
                .created(location)
                .body(response);
    }


    @GetMapping("/grupo/{grupoId}/resumo-anual")
    public ResponseEntity<ResumoAnualDTO> obterResumoAnual(
            @PathVariable @Positive Long grupoId,
            @RequestParam Integer ano
    ) {
        return ResponseEntity.ok(
                lancamentoService.obterResumoAnual(grupoId, ano)
        );
    }



    @PatchMapping("/{id}")
    public ResponseEntity<LancamentoResponseDTO> atualizar(
            @PathVariable @Positive Long id,
            @RequestHeader("If-Match") String ifMatch,
            @Valid @RequestBody AtualizarLancamentoRequestDTO request
    ) {
        return respostaComEtag(
                lancamentoService.atualizar(
                        id,
                        extrairVersao(ifMatch),
                        request
                )
        );
    }


    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<LancamentoResponseDTO> cancelar(
            @PathVariable @Positive Long id,
            @RequestHeader("If-Match") String ifMatch
    ) {
        return respostaComEtag(
                lancamentoService.cancelar(id, extrairVersao(ifMatch))
        );
    }

    private ResponseEntity<LancamentoResponseDTO> respostaComEtag(
            LancamentoResponseDTO response
    ) {
        return ResponseEntity.ok()
                .eTag(String.valueOf(response.version()))
                .body(response);
    }

    private Long extrairVersao(String ifMatch) {
        if (ifMatch == null || !ifMatch.matches("\"(0|[1-9][0-9]*)\"")) {
            throw new IllegalArgumentException(
                    "If-Match inválido. Informe a versão entre aspas, por exemplo: \"3\""
            );
        }

        try {
            return Long.parseLong(ifMatch.substring(1, ifMatch.length() - 1));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "A versão informada excede o limite permitido"
            );
        }
    }


}
