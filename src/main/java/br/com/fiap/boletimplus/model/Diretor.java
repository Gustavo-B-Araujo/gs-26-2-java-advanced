package br.com.fiap.boletimplus.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "Diretor")
public class Diretor extends PapelDaPessoa {

    protected Diretor() {
    }

    public Diretor(Pessoa pessoa) {
        super(pessoa);
    }
}
