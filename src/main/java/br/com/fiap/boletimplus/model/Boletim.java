package br.com.fiap.boletimplus.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/** Nota de um aluno (matrícula) numa avaliação. */
@Entity
@Table(name = "Boletim")
public class Boletim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long cod;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cod_turma_aluno")
    private TurmaAluno turmaAluno;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cod_avaliacao")
    private Avaliacao avaliacao;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal nota;

    protected Boletim() {
    }

    public Boletim(TurmaAluno turmaAluno, Avaliacao avaliacao, BigDecimal nota) {
        this.turmaAluno = turmaAluno;
        this.avaliacao = avaliacao;
        this.nota = nota;
    }

    public Long getCod() {
        return cod;
    }

    public TurmaAluno getTurmaAluno() {
        return turmaAluno;
    }

    public Avaliacao getAvaliacao() {
        return avaliacao;
    }

    public BigDecimal getNota() {
        return nota;
    }

    public void setNota(BigDecimal nota) {
        this.nota = nota;
    }
}
