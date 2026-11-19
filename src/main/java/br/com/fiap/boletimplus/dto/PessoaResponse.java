package br.com.fiap.boletimplus.dto;

import br.com.fiap.boletimplus.model.Pessoa;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public record PessoaResponse(
        Long cod,
        String nome,
        @JsonProperty("num_documento") String numDocumento,
        @JsonProperty("data_criacao") LocalDateTime dataCriacao) {

    public static PessoaResponse de(Pessoa pessoa) {
        return new PessoaResponse(pessoa.getCod(), pessoa.getNome(), pessoa.getNumDocumento(), pessoa.getDataCriacao());
    }
}
