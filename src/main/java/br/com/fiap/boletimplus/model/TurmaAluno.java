package br.com.fiap.boletimplus.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Matrícula do aluno numa turma. */
@Entity
@Table(name = "TurmaAluno")
public class TurmaAluno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long cod;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cod_turma")
    private Turma turma;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cod_aluno")
    private Aluno aluno;

    protected TurmaAluno() {
    }

    public TurmaAluno(Turma turma, Aluno aluno) {
        this.turma = turma;
        this.aluno = aluno;
    }

    public Long getCod() {
        return cod;
    }

    public Turma getTurma() {
        return turma;
    }

    public Aluno getAluno() {
        return aluno;
    }
}
