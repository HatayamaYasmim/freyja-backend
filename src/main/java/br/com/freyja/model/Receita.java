
package br.com.freyja.model;

import br.com.freyja.enums.StatusReceita;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "receita")
public class Receita {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "grupo_id", nullable = false)
    private Grupo grupo;

    @Column(name = "descricao", nullable = false, length = 255)
    private String descricao;

    @Column(
            name = "valor_previsto",
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal valorPrevisto;

    @Column(
            name = "valor_recebido",
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal valorRecebido = BigDecimal.ZERO;

    @Column(name = "competencia", nullable = false)
    private LocalDate competencia;

    @Column(name = "data_prevista", nullable = false)
    private LocalDate dataPrevista;

    @Column(name = "data_recebimento")
    private LocalDate dataRecebimento;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StatusReceita status = StatusReceita.PENDENTE;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;
}
