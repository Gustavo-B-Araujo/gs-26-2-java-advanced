package br.com.fiap.boletimplus.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "Aluno")
public class Aluno extends PapelDaPessoa {

    protected Aluno() {
    }

    public Aluno(Pessoa pessoa) {
        super(pessoa);
    }
}
