package br.com.fiap.boletimplus.model;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import org.hibernate.annotations.Generated;

import java.time.LocalDateTime;

@MappedSuperclass
public abstract class Enumerador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long cod;

    @Column(nullable = false, unique = true)
    private String enumerador;

    @Generated
    @Column(name = "data_criacao", insertable = false, updatable = false)
    private LocalDateTime dataCriacao;

    public Long getCod() {
        return cod;
    }

    public String getEnumerador() {
        return enumerador;
    }

    public void setEnumerador(String enumerador) {
        this.enumerador = enumerador;
    }

    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }
}
