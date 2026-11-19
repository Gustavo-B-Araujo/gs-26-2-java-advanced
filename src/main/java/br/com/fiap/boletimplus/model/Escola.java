package br.com.fiap.boletimplus.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "Escola")
public class Escola {

    @Id
    private Long cod;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cod_diretor")
    private Diretor diretor;

    @Column(nullable = false, length = 100)
    private String nome;

    protected Escola() {
    }

    public Long getCod() {
        return cod;
    }

    public Diretor getDiretor() {
        return diretor;
    }

    public String getNome() {
        return nome;
    }
}
