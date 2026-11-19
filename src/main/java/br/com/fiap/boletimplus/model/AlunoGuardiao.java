package br.com.fiap.boletimplus.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** Vínculo entre responsável e aluno, com o parentesco. */
@Entity
@Table(name = "AlunoGuardiao", uniqueConstraints = @UniqueConstraint(columnNames = {"cod_guardiao", "cod_aluno"}))
public class AlunoGuardiao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long cod;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cod_guardiao")
    private Guardiao guardiao;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cod_aluno")
    private Aluno aluno;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cod_tipo_parentesco")
    private TipoParentesco tipoParentesco;

    protected AlunoGuardiao() {
    }

    public AlunoGuardiao(Guardiao guardiao, Aluno aluno, TipoParentesco tipoParentesco) {
        this.guardiao = guardiao;
        this.aluno = aluno;
        this.tipoParentesco = tipoParentesco;
    }

    public Long getCod() {
        return cod;
    }

    public Guardiao getGuardiao() {
        return guardiao;
    }

    public Aluno getAluno() {
        return aluno;
    }

    public TipoParentesco getTipoParentesco() {
        return tipoParentesco;
    }

    public void setTipoParentesco(TipoParentesco tipoParentesco) {
        this.tipoParentesco = tipoParentesco;
    }
}
