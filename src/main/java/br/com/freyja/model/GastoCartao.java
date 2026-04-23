package br.com.freyja.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.sql.Timestamp;

@Entity
@Table(name = "gastos_cartao")
@Data
public class GastoCartao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cartao_id", nullable = false)
    private Cartao cartao;

    @CreationTimestamp
    @Column(name = "data_criacao", nullable = false)
    private Timestamp dataCriacao;

    @Column(nullable = false)
    private Integer parcela; // número da parcela atual

    @Column(name = "quantidade_parcelas", nullable = false)
    private Integer quantidadeParcelas;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal valor;

    @Column(nullable = false)
    private String usuario;

    @Column(nullable = false, length = 1)
    private String status;
    @Column(length = 255)
    private String descricao;
}
