package br.com.fiap.boletimplus.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "Guardiao")
public class Guardiao extends PapelDaPessoa {

    protected Guardiao() {
    }

    public Guardiao(Pessoa pessoa) {
        super(pessoa);
    }
}
