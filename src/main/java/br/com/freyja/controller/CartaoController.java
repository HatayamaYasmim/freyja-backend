package br.com.freyja.controller;

import br.com.freyja.dto.CartaoListagemResponseDTO;
import br.com.freyja.dto.CartaoRequestDTO;
import br.com.freyja.dto.CartaoResponseDTO;
import br.com.freyja.model.Cartao;
import br.com.freyja.service.CartaoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/cartao")
@CrossOrigin(origins = "*")
public class CartaoController {
    private final CartaoService service;

    public CartaoController(CartaoService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Cartao> criar(@RequestBody @Valid CartaoRequestDTO dto) {
        return ResponseEntity.ok(service.create(dto));
    }

    @GetMapping
    public  ResponseEntity<CartaoListagemResponseDTO> listAll() {
        List<Cartao> cartoes = service.listarTodos();
        List<CartaoResponseDTO> response = cartoes.stream()
                .map(cartao -> new CartaoResponseDTO(
                        cartao.getId(),
                        cartao.getNome(),
                        cartao.getDiaVencimento(),
                        cartao.getLimite(),
                        cartao.getStatus(),
                        cartao.getProprietario()
                ))
                .toList();

        Long totalCartoes = (long) cartoes.size();

        BigDecimal limiteTotal = cartoes.stream()
                .map(Cartao::getLimite)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        CartaoListagemResponseDTO result =
                new CartaoListagemResponseDTO(response, totalCartoes, limiteTotal);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CartaoResponseDTO> getById(@PathVariable Long id) {
        return service.buscarPorId(id)
                .map(cartao -> new CartaoResponseDTO(
                        cartao.getId(),
                        cartao.getNome(),
                        cartao.getDiaVencimento(),
                        cartao.getLimite(),
                        cartao.getStatus(),
                        cartao.getProprietario()
                ))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.deletar(id);
    }

    @GetMapping("/total")
    public ResponseEntity<Long> totalCartoes() {
        return ResponseEntity.ok(service.totalCartoes());
    }

    @PutMapping("/{id}")
    public ResponseEntity<CartaoResponseDTO> atualizar(
            @PathVariable Long id,
            @RequestBody @Valid CartaoRequestDTO dto
    ) {
        Cartao cartao = service.atualizar(id, dto);

        CartaoResponseDTO response = new CartaoResponseDTO(
                cartao.getId(),
                cartao.getNome(),
                cartao.getDiaVencimento(),
                cartao.getLimite(),
                cartao.getStatus(),
                cartao.getProprietario()
        );

        return ResponseEntity.ok(response);
    }
}
