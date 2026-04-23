package br.com.freyja.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "cartao")
@Data
public class Cartao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private  String nome;

    @Column(name = "dia_vencimento", nullable = false)
    private Integer diaVencimento;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal limite;

    @Column(nullable = false, length = 1)
    private String status; // A = ativo | I = inativo

    @Column(nullable = false)
    private String proprietario;

    @OneToMany(mappedBy = "cartao", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GastoCartao> gastos;
}
