package br.com.freyja.dto;

import br.com.freyja.enums.OrigemImportacao;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class GastoCartaoPreviewDTO {
    private LocalDate dataCompra;
    private String descricao;
    private BigDecimal valor;
    private Integer parcela;
    private Integer quantidadeParcelas;
    private String idExterno;
    private Boolean importar = true;
    private Boolean duplicado = false;
    private OrigemImportacao origem;
    private Long categoriaId;
}
