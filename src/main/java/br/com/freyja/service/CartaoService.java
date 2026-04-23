package br.com.freyja.service;

import br.com.freyja.dto.CartaoRequestDTO;
import br.com.freyja.model.Cartao;
import br.com.freyja.repository.CartaoRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CartaoService {
    private  final CartaoRepository cartaoRepo;

    public CartaoService(CartaoRepository cartaoRepo) {
        this.cartaoRepo = cartaoRepo;
    }

    @Transactional
    public Cartao create(CartaoRequestDTO req) {
        Cartao cartao = new Cartao();
        cartao.setNome(req.nome());
        cartao.setDiaVencimento(req.diaVencimento());
        cartao.setLimite(req.limite());
        cartao.setStatus(req.status() == null ? "A" : req.status());
        cartao.setProprietario(req.proprietario());

        return cartaoRepo.save(cartao);
    }

    public List<Cartao> listarTodos() {
        return cartaoRepo.findAll();
    }

    public Optional<Cartao> buscarPorId(Long id) {
        return cartaoRepo.findById(id);
    }

    public void deletar(Long id) {
        cartaoRepo.deleteById(id);
    }

    public Long totalCartoes() {
        return cartaoRepo.count();
    }

    public Cartao atualizar(Long id, CartaoRequestDTO dto) {
        Cartao cartao = cartaoRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Cartão não encontrado"));

        cartao.setNome(dto.nome());
        cartao.setDiaVencimento(dto.diaVencimento());
        cartao.setLimite(dto.limite());
        cartao.setStatus(
                dto.status() == null || dto.status().isBlank() ? "A" : dto.status()
        );
        cartao.setProprietario(dto.proprietario());

        return cartaoRepo.save(cartao);
    }
}
