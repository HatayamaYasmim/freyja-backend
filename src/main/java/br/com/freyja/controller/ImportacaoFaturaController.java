package br.com.freyja.controller;

import br.com.freyja.dto.GastoCartaoPreviewDTO;
import br.com.freyja.model.GastoCartao;
import br.com.freyja.service.GastoCartaoService;
import br.com.freyja.service.ImportacaoFaturaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/importacoes/faturas")
@CrossOrigin(origins = "*")
public class ImportacaoFaturaController {
    private final ImportacaoFaturaService importacaoFaturaService;
    private final GastoCartaoService gastoCartaoService;

    public ImportacaoFaturaController(
             ImportacaoFaturaService importacaoFaturaService,
            GastoCartaoService gastoCartaoService
    ) {
        this.importacaoFaturaService = importacaoFaturaService;
        this.gastoCartaoService = gastoCartaoService;
    }

    @PostMapping("/preview-pdf")
    public ResponseEntity<List<GastoCartaoPreviewDTO>> previewPDF(
            @RequestParam("file") MultipartFile file
    ) {
        return ResponseEntity.ok(importacaoFaturaService.previewPDF(file));
    }

//    @PostMapping("/confirmar/{cartaoId}")
//    public ResponseEntity<List<GastoCartao>> confirmarImportacao(
//            @PathVariable Long cartaoId,
//            @RequestBody List<GastoCartaoPreviewDTO> itens
//    ) {
//        return ResponseEntity.ok(gastoCartaoService.salvarImportacao(cartaoId, itens));
//    }
}
