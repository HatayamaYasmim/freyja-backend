package br.com.freyja.controller;


import br.com.freyja.dto.contarecorrente.AtualizarContaRecorrenteRequestDTO;
import br.com.freyja.dto.contarecorrente.ContaRecorrenteRequestDTO;
import br.com.freyja.dto.contarecorrente.ContaRecorrenteResponseDTO;
import br.com.freyja.dto.contarecorrente.EncerrarContaRecorrenteRequestDTO;
import br.com.freyja.service.ContaRecorrenteService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/contas-recorrentes")
@Validated
public class ContaRecorrenteController {
    private final ContaRecorrenteService service;

    public ContaRecorrenteController(ContaRecorrenteService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ContaRecorrenteResponseDTO> cadastrar(@Valid @RequestBody ContaRecorrenteRequestDTO dto) {
        ContaRecorrenteResponseDTO response = service.cadastrar(dto);

        URI location = URI.create(
                "/contas-recorrentes/" + response.id()
        );

        return ResponseEntity
                .created(location)
                .body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ContaRecorrenteResponseDTO> buscarPorId(
            @PathVariable @Positive Long id
    ) {
        return ResponseEntity.ok(service.findByGrupo_Id(id));
    }

    @GetMapping("/grupo/{grupoId}")
    public ResponseEntity<List<ContaRecorrenteResponseDTO>> listarPorGrupo(
            @PathVariable @Positive Long grupoId
    ) {
        return ResponseEntity.ok(service.listarPorGrupo(grupoId));
    }


    @PatchMapping("/{id}")
    public ResponseEntity<ContaRecorrenteResponseDTO> atualizar(
            @PathVariable @Positive Long id,
            @Valid @RequestBody AtualizarContaRecorrenteRequestDTO request
    ) {
        return ResponseEntity.ok(
                service.atualizar(id, request)
        );
    }

    @PatchMapping("/{id}/desativar")
    public ResponseEntity<ContaRecorrenteResponseDTO> desativar(@PathVariable @Positive Long id){
        return ResponseEntity.ok(
                service.desativar(id)
        );
    }


    @PatchMapping("/{id}/encerrar")
    public ResponseEntity<ContaRecorrenteResponseDTO> encerrar(
            @PathVariable @Positive Long id,
            @Valid @RequestBody EncerrarContaRecorrenteRequestDTO request
    ) {
        return ResponseEntity.ok(
                service.encerrar(id, request)
        );
    }



}
