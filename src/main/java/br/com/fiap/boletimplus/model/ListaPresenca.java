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
@Table(name = "ListaPresenca")
public class ListaPresenca {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long cod;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cod_turma_aluno")
    private TurmaAluno turmaAluno;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cod_tipo_presenca")
    private TipoPresenca tipoPresenca;

    @Column(length = 150)
    private String observacao;

    @Column(name = "data_presenca", nullable = false)
    private LocalDate dataPresenca;

    protected ListaPresenca() {
    }

    public ListaPresenca(TurmaAluno turmaAluno, TipoPresenca tipoPresenca, String observacao, LocalDate dataPresenca) {
        this.turmaAluno = turmaAluno;
        this.tipoPresenca = tipoPresenca;
        this.observacao = observacao;
        this.dataPresenca = dataPresenca;
    }

    public Long getCod() {
        return cod;
    }

    public TurmaAluno getTurmaAluno() {
        return turmaAluno;
    }

    public TipoPresenca getTipoPresenca() {
        return tipoPresenca;
    }

    public String getObservacao() {
        return observacao;
    }

    public LocalDate getDataPresenca() {
        return dataPresenca;
    }

    public void alterar(TipoPresenca tipoPresenca, String observacao) {
        this.tipoPresenca = tipoPresenca;
        this.observacao = observacao;
    }
}
