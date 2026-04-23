package br.com.freyja.dto;

import java.math.BigDecimal;

public class CartaoResponseDTO {
    private Long id;
    private String nome;
    private Integer diaVencimento;
    private BigDecimal limite;
    private String status;
    private String proprietario;

    public CartaoResponseDTO(Long id, String nome, Integer diaVencimento,
                             BigDecimal limite, String status, String proprietario) {
        this.id = id;
        this.nome = nome;
        this.diaVencimento = diaVencimento;
        this.limite = limite;
        this.status = status;
        this.proprietario = proprietario;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public Integer getDiaVencimento() { return diaVencimento; }
    public BigDecimal getLimite() { return limite; }
    public String getStatus() { return status; }
    public String getProprietario() { return proprietario; }
}