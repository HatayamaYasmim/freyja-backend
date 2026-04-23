package br.com.freyja.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "grupo")
@Data
public class Grupo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;
}
