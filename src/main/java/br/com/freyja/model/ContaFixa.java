package br.com.freyja.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "conta_fixa")
@Data
public class ContaFixa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "id_grupo", nullable = false)
    private Grupo grupo;

    @ManyToOne
    @JoinColumn(name = "id_categoria", nullable = false)
    private Categoria categoria;

    @Column(nullable = false, length = 1)
    private String tipo; // 1- Fixo | 2 = Variavel

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal valor;

    @Column(nullable = false)
    private String usuarioCriacao;

    @Column(nullable = false)
    @CreationTimestamp
    private LocalDateTime dataCriacao;

    @Column(nullable = false)
    private LocalDate dataConta;

    @Column(nullable = false)
    private boolean gastoRecorrente;

    @Column
    private String descricao;

    @Column(name = "status_pagamento", nullable = false, length = 1)
    private String statusPagamento;


}
