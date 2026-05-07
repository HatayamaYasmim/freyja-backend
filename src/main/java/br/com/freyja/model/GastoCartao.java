package br.com.freyja.model;

import br.com.freyja.enums.OrigemImportacao;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;

@Entity
@Table(
        name = "gastos_cartao",
        indexes = {
                @Index(
                        name = "uk_gastos_cartao_id_externo",
                        columnList = "id_externo",
                        unique = true
                ),
                @Index(
                        name = "idx_gastos_cartao_data_compra",
                        columnList = "data_compra"
                ),
                @Index(
                        name = "idx_gastos_cartao_cartao",
                        columnList = "cartao_id"
                )
        }
)
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

    @Column(name = "data_compra", nullable = false)
    private LocalDate dataCompra;

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

    // Evita import duplicado*
    @Column(name = "id_externo", length = 255)
    private String idExterno;

    @Enumerated(EnumType.STRING)
    @Column(name = "origem", length = 20)
    private OrigemImportacao origem;

    @ManyToOne
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;
}
