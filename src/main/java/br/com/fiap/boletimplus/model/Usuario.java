package br.com.fiap.boletimplus.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "Usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long cod;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cod_status")
    private StatusUsuario status;

    @OneToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "cod_pessoa", unique = true)
    private Pessoa pessoa;

    @Column(name = "senha_hash", nullable = false, length = 60)
    private String senhaHash;

    protected Usuario() {
    }

    public Usuario(Pessoa pessoa, StatusUsuario status, String senhaHash) {
        this.pessoa = pessoa;
        this.status = status;
        this.senhaHash = senhaHash;
    }

    public Long getCod() {
        return cod;
    }

    public StatusUsuario getStatus() {
        return status;
    }

    public Pessoa getPessoa() {
        return pessoa;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public void setSenhaHash(String senhaHash) {
        this.senhaHash = senhaHash;
    }
}
