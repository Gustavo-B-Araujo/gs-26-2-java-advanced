package br.com.fiap.boletimplus.model;

import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToOne;

/** Diretor, Professor, Guardiao e Aluno: cada papel é uma tabela que aponta para a Pessoa. */
@MappedSuperclass
public abstract class PapelDaPessoa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long cod;

    @OneToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "cod_pessoa", unique = true)
    private Pessoa pessoa;

    protected PapelDaPessoa() {
    }

    protected PapelDaPessoa(Pessoa pessoa) {
        this.pessoa = pessoa;
    }

    public Long getCod() {
        return cod;
    }

    public Pessoa getPessoa() {
        return pessoa;
    }
}
