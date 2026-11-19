package br.com.fiap.boletimplus.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "Professor")
public class Professor extends PapelDaPessoa {

    protected Professor() {
    }

    public Professor(Pessoa pessoa) {
        super(pessoa);
    }
}
