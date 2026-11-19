package br.com.fiap.boletimplus.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "Turma")
public class Turma {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long cod;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cod_escola")
    private Escola escola;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cod_tipo_curso")
    private TipoCurso tipoCurso;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cod_professor")
    private Professor professor;

    @Column(name = "semestre_referencia", nullable = false)
    private Integer semestreReferencia;

    @Column(name = "ano_referencia", nullable = false)
    private Integer anoReferencia;

    protected Turma() {
    }

    public Turma(Escola escola, TipoCurso tipoCurso, Professor professor, Integer semestreReferencia,
                 Integer anoReferencia) {
        this.escola = escola;
        this.tipoCurso = tipoCurso;
        this.professor = professor;
        this.semestreReferencia = semestreReferencia;
        this.anoReferencia = anoReferencia;
    }

    public Long getCod() {
        return cod;
    }

    public Escola getEscola() {
        return escola;
    }

    public TipoCurso getTipoCurso() {
        return tipoCurso;
    }

    public Professor getProfessor() {
        return professor;
    }

    public Integer getSemestreReferencia() {
        return semestreReferencia;
    }

    public Integer getAnoReferencia() {
        return anoReferencia;
    }
}
