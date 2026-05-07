package br.com.freyja.service;

import br.com.freyja.dto.GastoCartaoDTO;
import br.com.freyja.dto.GastoCartaoPreviewDTO;
import br.com.freyja.enums.OrigemImportacao;
import br.com.freyja.model.Cartao;
import br.com.freyja.model.Categoria;
import br.com.freyja.model.GastoCartao;
import br.com.freyja.repository.CartaoRepository;
import br.com.freyja.repository.CategoriaRepository;
import br.com.freyja.repository.GastosCartaoRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class GastoCartaoService {
    private final GastosCartaoRepository gastoRepo;
    private final CartaoRepository cartaoRepo;
    private final CategoriaRepository categoriaRepo;

    public GastoCartaoService(GastosCartaoRepository gastoRepo, CartaoRepository cartaoRepo, CategoriaRepository categoriaRepo, CategoriaRepository categoriaRepo1) {
        this.gastoRepo = gastoRepo;
        this.cartaoRepo = cartaoRepo;
        this.categoriaRepo = categoriaRepo1;
    }

    @Transactional
    public List<GastoCartao> parcelar(Long cartaoId, GastoCartaoDTO req) {
        Cartao cartao = cartaoRepo.findById(cartaoId)
                .orElseThrow(() -> new IllegalArgumentException("Cartão não encontrado: " + cartaoId));
        int n = req.getQuantidadeParcelas();
        BigDecimal total = req.getValorTotal().setScale(2, RoundingMode.HALF_UP);
        BigDecimal base = total.divide(BigDecimal.valueOf(n), 2, RoundingMode.DOWN);

        // diferença (centavos) pra ajustar e fechar total exato
        BigDecimal somaBase = base.multiply(BigDecimal.valueOf(n)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal diff = total.subtract(somaBase); // 0.00 até 0.99 (ou negativo raramente)

        String status = (req.getStatus() == null || req.getStatus().isBlank()) ? "A" : req.getStatus();
        List<GastoCartao> parcelas = new ArrayList<>(n);

        for (int i = 1; i <= n; i++) {
            GastoCartao g = new GastoCartao();
            g.setCartao(cartao);
            g.setUsuario(req.getUsuario());
            g.setDescricao(req.getDescricao());
            g.setQuantidadeParcelas(n);
            g.setParcela(i);
            g.setStatus(status);
            g.setDataCompra(req.getDataCompra());
//            g.setOrigem(OrigemLancamento.MANUAL);
            g.setIdExterno(null);

            // ajusta a diferença na 1ª parcela (padrão simples e comum)
            BigDecimal valorParcela = (i == 1) ? base.add(diff) : base;
            g.setValor(valorParcela.setScale(2, RoundingMode.HALF_UP));

            parcelas.add(g);
        }

        return gastoRepo.saveAll(parcelas);
    }

    @Transactional
    public GastoCartao atualizar(Long id, GastoCartaoDTO dto) {

        GastoCartao g = gastoRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Gasto não encontrado: " + id));

        g.setDescricao(dto.getDescricao());
        g.setUsuario(dto.getUsuario());

        if (dto.getStatus() != null && !dto.getStatus().isBlank()) {
            g.setStatus(dto.getStatus());
        }

        return gastoRepo.save(g);
    }


    @Transactional
    public List<GastoCartao> salvarImportacao(
            Long cartaoId,
            List<GastoCartaoPreviewDTO> itens
    ) {
        Cartao cartao = cartaoRepo.findById(cartaoId)
                .orElseThrow(() -> new IllegalArgumentException("Cartão não encontrado: " + cartaoId));

        List<GastoCartao> gastos = itens.stream()
                .filter(GastoCartaoPreviewDTO::getImportar)
                .filter(item -> item.getIdExterno() == null || !gastoRepo.existsByIdExterno(item.getIdExterno()))
                .map(item -> {
                    GastoCartao gasto = new GastoCartao();

                    gasto.setCartao(cartao);
                    gasto.setDataCompra(item.getDataCompra());
                    gasto.setDescricao(item.getDescricao());
                    gasto.setValor(item.getValor());
                    gasto.setParcela(item.getParcela());
                    gasto.setQuantidadeParcelas(item.getQuantidadeParcelas());
                    gasto.setIdExterno(item.getIdExterno());
                    gasto.setUsuario("admin"); //
                    gasto.setStatus("A");
                    gasto.setOrigem(OrigemImportacao.PDF);
                    if (item.getCategoriaId() != null) {

                        Categoria categoria = categoriaRepo.findById(item.getCategoriaId())
                                .orElseThrow(() ->
                                        new IllegalArgumentException("Categoria não encontrada"));

                        gasto.setCategoria(categoria);
                    }

                    return gasto;
                })
                .toList();

        return gastoRepo.saveAll(gastos);
    }
}
