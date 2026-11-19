package br.com.fiap.boletimplus.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "Avaliacao")
public class Avaliacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long cod;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cod_turma")
    private Turma turma;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cod_tipo_nota")
    private TipoNota tipoNota;

    @Column(nullable = false, length = 50)
    private String nome;

    @Column(name = "data_avaliacao")
    private LocalDate dataAvaliacao;

    protected Avaliacao() {
    }

    public Avaliacao(Turma turma, TipoNota tipoNota, String nome, LocalDate dataAvaliacao) {
        this.turma = turma;
        this.tipoNota = tipoNota;
        this.nome = nome;
        this.dataAvaliacao = dataAvaliacao;
    }

    public Long getCod() {
        return cod;
    }

    public Turma getTurma() {
        return turma;
    }

    public TipoNota getTipoNota() {
        return tipoNota;
    }

    public String getNome() {
        return nome;
    }

    public LocalDate getDataAvaliacao() {
        return dataAvaliacao;
    }

    public void alterar(TipoNota tipoNota, String nome, LocalDate dataAvaliacao) {
        this.tipoNota = tipoNota;
        this.nome = nome;
        this.dataAvaliacao = dataAvaliacao;
    }
}
