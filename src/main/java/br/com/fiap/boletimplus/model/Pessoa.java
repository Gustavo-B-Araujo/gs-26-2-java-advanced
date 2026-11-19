package br.com.fiap.boletimplus.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Generated;

import java.time.LocalDateTime;

@Entity
@Table(name = "Pessoa")
public class Pessoa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long cod;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(name = "num_documento", nullable = false, unique = true, length = 11)
    private String numDocumento;

    @Generated
    @Column(name = "data_criacao", insertable = false, updatable = false)
    private LocalDateTime dataCriacao;

    protected Pessoa() {
    }

    public Pessoa(String nome, String numDocumento) {
        this.nome = nome;
        this.numDocumento = numDocumento;
    }

    public Long getCod() {
        return cod;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getNumDocumento() {
        return numDocumento;
    }

    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }
}
